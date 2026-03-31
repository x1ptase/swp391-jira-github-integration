package com.swp391.backend.service.monitoring;

import com.swp391.backend.config.MonitoringConfig;
import com.swp391.backend.dto.monitoring.request.MonitoringFilterRequest;
import com.swp391.backend.dto.monitoring.response.GroupMonitoringDTO;
import com.swp391.backend.dto.monitoring.shared.MonitoringDateRange;
import com.swp391.backend.entity.monitoring.HealthStatus;
import com.swp391.backend.entity.monitoring.PrimaryReason;
import com.swp391.backend.repository.monitoring.GroupRawProjection;
import com.swp391.backend.repository.monitoring.MonitoringAggregationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service giám sát sức khoẻ nhóm sinh viên theo từng lớp học.
 *
 * <h4>Trách nhiệm:</h4>
 * <ul>
 *   <li>Gọi một query single-shot {@code getGroupRawByClassId} để lấy toàn bộ
 *       dữ liệu cần thiết của tất cả groups trong một class — tránh N+1.</li>
 *   <li>Tính toán {@link HealthStatus} và {@link PrimaryReason} theo quy tắc
 *       nghiệp vụ đã định nghĩa (ủy quyền cho {@link MonitoringRuleService}).</li>
 *   <li>Map kết quả thành {@link GroupMonitoringDTO} để trả về API.</li>
 * </ul>
 *
 * <h4>Phân chia trách nhiệm:</h4>
 * <ul>
 *   <li>Service này chịu trách nhiệm <b>orchestration</b> (fetch → map → classify).</li>
 *   <li>Tất cả <b>business rule</b> (ngưỡng critical/warning) nằm trong
 *       {@link MonitoringRuleService} để đảm bảo consistency với các API khác.</li>
 *   <li>Resolve khoảng thời gian được ủy quyền cho {@link MonitoringDateRangeService}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupMonitoringService {

    private final MonitoringAggregationRepository aggregationRepository;
    private final MonitoringDateRangeService dateRangeService;
    private final MonitoringConfig config;

    // ────────────────────────────────────────────────────────────────────────
    // Public API
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Trả về danh sách {@link GroupMonitoringDTO} cho tất cả các nhóm trong một lớp.
     *
     * <p>Khoảng thời gian được resolve từ {@code filter}:
     * <ol>
     *   <li>Nếu {@code fromDate + toDate} có giá trị → dùng trực tiếp.</li>
     *   <li>Nếu {@code lastNDays} có giá trị → N ngày gần đây.</li>
     *   <li>Không có gì → dùng default window từ config (mặc định 7 ngày).</li>
     * </ol>
     *
     * @param classId ID của lớp học cần giám sát
     * @param filter  filter thời gian (có thể null)
     * @return danh sách DTO đã xếp loại sức khoẻ, sắp xếp theo healthStatus giảm dần
     *         (CRITICAL trước, sau đó WARNING, CLOSED, HEALTHY)
     */
    public List<GroupMonitoringDTO> getByClass(Long classId, MonitoringFilterRequest filter) {
        MonitoringDateRange range = dateRangeService.resolve(filter);
        return getByClassAndRange(classId, range.getFrom(), range.getTo());
    }

    /**
     * Overload dùng directly khi đã có {@link MonitoringDateRange}.
     * Hữu ích khi gọi từ các service khác đã resolve range trước.
     *
     * @param classId   ID lớp học
     * @param dateRange khoảng thời gian đã resolve
     * @return danh sách {@link GroupMonitoringDTO}
     */
    public List<GroupMonitoringDTO> getByClass(Long classId, MonitoringDateRange dateRange) {
        return getByClassAndRange(classId, dateRange.getFrom(), dateRange.getTo());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Core logic
    // ────────────────────────────────────────────────────────────────────────

    private List<GroupMonitoringDTO> getByClassAndRange(
            Long classId, LocalDateTime fromDate, LocalDateTime toDate) {

        List<GroupRawProjection> raws =
                aggregationRepository.getGroupRawByClassId(classId, fromDate, toDate);

        LocalDateTime now = LocalDateTime.now();

        return raws.stream()
                .map(raw -> toDto(raw, now))
                .sorted(this::compareByHealth)
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Mapping
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Map một {@link GroupRawProjection} sang {@link GroupMonitoringDTO}.
     *
     * <p>Các bước:
     * <ol>
     *   <li>Tính {@code activeMemberRatio}.</li>
     *   <li>Phát hiện sync stale.</li>
     *   <li>Xác định {@link HealthStatus} — ưu tiên CLOSED nếu nhóm đã đóng.</li>
     *   <li>Chọn {@link PrimaryReason} primary duy nhất.</li>
     *   <li>Format {@code membersText}.</li>
     * </ol>
     */
    private GroupMonitoringDTO toDto(GroupRawProjection raw, LocalDateTime now) {

        // ── 1. Lấy raw metrics ────────────────────────────────────────────
        long totalMembers  = nullToZero(raw.getTotalMembers());
        long activeMembers = nullToZero(raw.getActiveMembers());
        long overdueTasks  = nullToZero(raw.getOverdueTasks());
        long totalCommits  = nullToZero(raw.getTotalCommits());

        LocalDateTime lastActivityAt = raw.getLastCommitAt();
        boolean hasTopic = raw.getTopicName() != null;

        // ── 2. Tính toán tỉ lệ ───────────────────────────────────────────
        double activeMemberRatio = totalMembers > 0
                ? (double) activeMembers / totalMembers
                : 0.0;

        // ── 3. Kiểm tra sync staleness ────────────────────────────────────
        // "Stale" = chưa sync bao giờ HOẶC lần sync gần nhất bị FAILED
        // HOẶC sync thành công nhưng quá cũ (> staleActivityDays)
        boolean githubSyncStale = isSyncStale(raw.getGithubSyncStatus(), raw.getGithubSyncStartedAt(), now);
        boolean jiraSyncStale   = isSyncStale(raw.getJiraSyncStatus(),   raw.getJiraSyncStartedAt(),   now);

        // ── 4. Phân loại HealthStatus ─────────────────────────────────────
        HealthStatus healthStatus = classifyHealth(
                raw.getGroupStatus(),
                totalCommits,
                activeMemberRatio,
                (int) overdueTasks,
                lastActivityAt,
                githubSyncStale,
                jiraSyncStale,
                hasTopic,
                (int) totalMembers);

        // ── 5. Chọn PrimaryReason (chỉ tính nếu không CLOSED) ────────────
        PrimaryReason primaryReason = "CLOSED".equalsIgnoreCase(raw.getGroupStatus())
                ? PrimaryReason.STABLE
                : choosePrimaryReason(
                        totalCommits, activeMemberRatio, (int) overdueTasks,
                        lastActivityAt, githubSyncStale, jiraSyncStale, hasTopic, now);

        // ── 6. Format membersText ─────────────────────────────────────────
        String membersText = activeMembers + "/" + totalMembers + " active";

        // ── 7. Build DTO ──────────────────────────────────────────────────
        return GroupMonitoringDTO.builder()
                .groupId(raw.getGroupId())
                .groupName(raw.getGroupName())
                .groupStatus(raw.getGroupStatus())
                .topicName(raw.getTopicName())
                .totalMembers((int) totalMembers)
                .activeMembers((int) activeMembers)
                .membersText(membersText)
                .overdueTasks((int) overdueTasks)
                .totalCommits(totalCommits)
                .lastActivityAt(lastActivityAt)
                .githubSyncStatus(raw.getGithubSyncStatus())
                .githubSyncStartedAt(raw.getGithubSyncStartedAt())
                .jiraSyncStatus(raw.getJiraSyncStatus())
                .jiraSyncStartedAt(raw.getJiraSyncStartedAt())
                .healthStatus(healthStatus)
                .primaryReason(primaryReason)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Health classification
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Xác định {@link HealthStatus} theo quy tắc nghiệp vụ.
     *
     * <h4>Thứ tự kiểm tra:</h4>
     * <ol>
     *   <li><b>CLOSED</b>: GroupStatus = 'CLOSED' → dừng tại đây.</li>
     *   <li><b>CRITICAL</b> (bất kỳ điều kiện nào):
     *       <ul>
     *         <li>totalCommits == 0</li>
     *         <li>activeMemberRatio &lt; criticalActiveMemberRatio (0.25)</li>
     *         <li>overdueTasks &ge; criticalOverdueTasks (5)</li>
     *         <li>lastActivityAt &gt; staleActivityDays ngày (7) hoặc null</li>
     *         <li>githubSyncStale = true HOẶC jiraSyncStale = true</li>
     *       </ul>
     *   </li>
     *   <li><b>WARNING</b> (không CRITICAL, bất kỳ điều kiện nào):
     *       <ul>
     *         <li>overdueTasks trong [warningOverdueTasksMin, criticalOverdueTasks) = [2,4]</li>
     *         <li>activeMemberRatio &lt; warningActiveMemberRatio (0.50)</li>
     *         <li>topic chưa được gán (topicName == null)</li>
     *       </ul>
     *   </li>
     *   <li><b>HEALTHY</b>: còn lại.</li>
     * </ol>
     */
    private HealthStatus classifyHealth(
            String groupStatus,
            long totalCommits,
            double activeMemberRatio,
            int overdueTasks,
            LocalDateTime lastActivityAt,
            boolean githubSyncStale,
            boolean jiraSyncStale,
            boolean hasTopic,
            int totalMembers) {

        // ── CLOSED ────────────────────────────────────────────────────────
        if ("CLOSED".equalsIgnoreCase(groupStatus)) {
            return HealthStatus.CLOSED;
        }

        // ── CRITICAL ──────────────────────────────────────────────────────
        if (totalCommits == 0) return HealthStatus.CRITICAL;
        if (activeMemberRatio < config.getCriticalActiveMemberRatio()) return HealthStatus.CRITICAL;
        if (overdueTasks >= config.getCriticalOverdueTasks()) return HealthStatus.CRITICAL;
        if (isActivityStale(lastActivityAt)) return HealthStatus.CRITICAL;
        if (githubSyncStale || jiraSyncStale) return HealthStatus.CRITICAL;

        // ── WARNING ───────────────────────────────────────────────────────
        if (overdueTasks >= config.getWarningOverdueTasksMin()) return HealthStatus.WARNING;
        if (activeMemberRatio < config.getWarningActiveMemberRatio()) return HealthStatus.WARNING;
        if (!hasTopic) return HealthStatus.WARNING;

        // ── HEALTHY ───────────────────────────────────────────────────────
        return HealthStatus.HEALTHY;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Primary Reason
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Chọn lý do chính duy nhất theo thứ tự ưu tiên giảm dần.
     *
     * <p>Thứ tự ưu tiên:
     * <ol>
     *   <li>{@link PrimaryReason#NO_ACTIVITY_THIS_WEEK} – lastActivity &gt; 7 ngày</li>
     *   <li>{@link PrimaryReason#TOO_FEW_ACTIVE_MEMBERS} – ratio &lt; 0.50</li>
     *   <li>{@link PrimaryReason#TOO_MANY_OVERDUE_TASKS} – overdue &ge; 2</li>
     *   <li>{@link PrimaryReason#TOPIC_NOT_ASSIGNED} – chưa có topic</li>
     *   <li>{@link PrimaryReason#STALE_SYNC} – sync bị stale hoặc FAILED</li>
     *   <li>{@link PrimaryReason#STABLE} – không có lý do cảnh báo</li>
     * </ol>
     *
     * <p>Lý do {@code NO_ACTIVITY_THIS_WEEK} có độ ưu tiên cao nhất vì nó
     * phản ánh trực tiếp nguy cơ trễ tiến độ nghiêm trọng nhất.
     */
    private PrimaryReason choosePrimaryReason(
            long totalCommits,
            double activeMemberRatio,
            int overdueTasks,
            LocalDateTime lastActivityAt,
            boolean githubSyncStale,
            boolean jiraSyncStale,
            boolean hasTopic,
            LocalDateTime now) {

        // 1. Không có hoạt động trong tuần (totalCommits = 0 HOẶC lastActivity > 7 ngày)
        if (totalCommits == 0 || isActivityStale(lastActivityAt)) {
            return PrimaryReason.NO_ACTIVITY_THIS_WEEK;
        }

        // 2. Quá ít thành viên active (ratio < 50%)
        if (activeMemberRatio < config.getWarningActiveMemberRatio()) {
            return PrimaryReason.TOO_FEW_ACTIVE_MEMBERS;
        }

        // 3. Quá nhiều task overdue (>= 2)
        if (overdueTasks >= config.getWarningOverdueTasksMin()) {
            return PrimaryReason.TOO_MANY_OVERDUE_TASKS;
        }

        // 4. Chưa có topic
        if (!hasTopic) {
            return PrimaryReason.TOPIC_NOT_ASSIGNED;
        }

        // 5. Sync bị stale hoặc FAILED
        if (githubSyncStale || jiraSyncStale) {
            return PrimaryReason.STALE_SYNC;
        }

        // 6. Không có vấn đề
        return PrimaryReason.STABLE;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Sorting
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Comparator để sắp xếp DTO theo mức độ nghiêm trọng giảm dần:
     * CRITICAL → WARNING → CLOSED → HEALTHY.
     */
    private int compareByHealth(GroupMonitoringDTO a, GroupMonitoringDTO b) {
        return Integer.compare(healthOrder(a.getHealthStatus()), healthOrder(b.getHealthStatus()));
    }

    private int healthOrder(HealthStatus status) {
        if (status == null) return 99;
        return switch (status) {
            case CRITICAL -> 0;
            case WARNING  -> 1;
            case CLOSED   -> 2;
            case HEALTHY  -> 3;
        };
    }

    // ────────────────────────────────────────────────────────────────────────
    // Staleness helpers
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Kiểm tra commit activity có bị stale không.
     * Stale nếu {@code lastActivityAt == null} hoặc cũ hơn {@code staleActivityDays}.
     */
    private boolean isActivityStale(LocalDateTime lastActivityAt) {
        if (lastActivityAt == null) return true;
        return lastActivityAt.isBefore(
                LocalDateTime.now().minusDays(config.getStaleActivityDays()));
    }

    /**
     * Kiểm tra một nguồn sync (GITHUB/JIRA) có bị stale không.
     *
     * <p>Một sync được coi là stale nếu:
     * <ul>
     *   <li>Chưa có sync nào ({@code syncStatus == null})</li>
     *   <li>Lần sync gần nhất có status {@code "FAILED"}</li>
     *   <li>Lần sync thành công cuối cùng đã quá {@code staleActivityDays} ngày</li>
     * </ul>
     *
     * <p><b>Lưu ý:</b> {@code syncStatus} và {@code syncStartedAt} là của lần sync
     * <em>bất kỳ</em> gần nhất (kể cả FAILED), không phải chỉ SUCCESS.
     * Điều này đảm bảo phát hiện khi lần cuối cùng bị lỗi.
     */
    private boolean isSyncStale(String syncStatus, LocalDateTime syncStartedAt, LocalDateTime now) {
        // Chưa từng sync
        if (syncStatus == null || syncStartedAt == null) {
            return true;
        }

        // Sync gần nhất bị FAILED
        if ("FAILED".equalsIgnoreCase(syncStatus)) {
            return true;
        }

        // Sync gần nhất (SUCCESS hoặc RUNNING) nhưng đã quá cũ
        return syncStartedAt.isBefore(
                now.minusDays(config.getStaleActivityDays()));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Utility
    // ────────────────────────────────────────────────────────────────────────

    /** Chuyển null Long thành 0. */
    private long nullToZero(Long value) {
        return value != null ? value : 0L;
    }
}
