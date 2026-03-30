package com.swp391.backend.service.monitoring;

import com.swp391.backend.config.MonitoringConfig;
import com.swp391.backend.dto.monitoring.shared.GroupMonitoringMetrics;
import com.swp391.backend.entity.monitoring.ContributionStatus;
import com.swp391.backend.entity.monitoring.HealthStatus;
import com.swp391.backend.entity.monitoring.PrimaryReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service chứa toàn bộ business rule cho monitoring.
 * <p>
 * Đây là "rule engine" trung tâm – <b>không có logic rule nào được phép
 * nằm trải rải ở các service khác</b>. Tất cả các API monitoring
 * (BE-01 → BE-05) đều gọi qua đây để đảm bảo consistency.
 *
 * <h4>Không phụ thuộc vào database</h4>
 * Service này chỉ thực hiện tính toán thuần túy từ dữ liệu đã được
 * tổng hợp sẵn. Việc fetch dữ liệu được thực hiện bởi
 * {@link MonitoringAggregationService}.
 */
@Service
@RequiredArgsConstructor
public class MonitoringRuleService {

    private final MonitoringConfig config;

    // ────────────────────────────────────────────────────────────────────────
    // A. Student contribution status
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Tính mức đóng góp của sinh viên dựa trên số commit.
     *
     * <ul>
     *   <li>ACTIVE nếu commitCount &ge; {@code activeCommitThreshold} (mặc định: 2)</li>
     *   <li>LOW nếu commitCount &ge; {@code lowCommitThreshold} (mặc định: 1)</li>
     *   <li>NO_CONTRIBUTION nếu commitCount = 0</li>
     * </ul>
     *
     * @param commitCount số commit trong khoảng thời gian monitoring
     * @return trạng thái đóng góp
     */
    public ContributionStatus computeContributionStatus(long commitCount) {
        if (commitCount >= config.getActiveCommitThreshold()) {
            return ContributionStatus.ACTIVE;
        }
        if (commitCount >= config.getLowCommitThreshold()) {
            return ContributionStatus.LOW;
        }
        return ContributionStatus.NO_CONTRIBUTION;
    }

    // ────────────────────────────────────────────────────────────────────────
    // B. Group health computation
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Tính tình trạng sức khoẻ của một nhóm dựa trên các metrics.
     *
     * <h4>CRITICAL nếu bất kỳ điều kiện nào:</h4>
     * <ul>
     *   <li>totalCommits == 0</li>
     *   <li>activeMemberRatio &lt; criticalActiveMemberRatio (0.25)</li>
     *   <li>overdueTasks &ge; criticalOverdueTasks (5)</li>
     *   <li>lastActivityAt == null hoặc cũ hơn staleActivityDays (7 ngày)</li>
     *   <li>githubSyncStale == true HOẶC jiraSyncStale == true</li>
     * </ul>
     *
     * <h4>WARNING nếu không CRITICAL và bất kỳ điều kiện nào:</h4>
     * <ul>
     *   <li>totalCommits &lt; expectedCommitThreshold (max(4, totalMembers * 2))</li>
     *   <li>activeMemberRatio &lt; warningActiveMemberRatio (0.50)</li>
     *   <li>overdueTasks &ge; warningOverdueTasksMin (2)</li>
     *   <li>topContributorShare &ge; topContributorWarningRatio (0.70)</li>
     *   <li>hasTopic == false</li>
     * </ul>
     *
     * <h4>HEALTHY</h4>
     * Nếu không thuộc CRITICAL hay WARNING.
     *
     * @param totalCommits       tổng commit của nhóm trong monitoring window
     * @param activeMemberRatio  activeMember / totalMembers (0.0 nếu không có thành viên)
     * @param overdueTasks       số task overdue
     * @param lastActivityAt     thời điểm commit gần nhất (null nếu chưa có)
     * @param githubSyncStale    true nếu sync GitHub quá cũ hoặc chưa sync
     * @param jiraSyncStale      true nếu sync Jira quá cũ hoặc chưa sync
     * @param hasTopic           nhóm đã có topic chưa
     * @param topContributorShare tỉ lệ commit của top contributor / tổng
     * @param totalMembers       tổng số thành viên (dùng tính expected threshold)
     * @return HealthStatus đã tính
     */
    public HealthStatus computeGroupHealth(
            long totalCommits,
            double activeMemberRatio,
            int overdueTasks,
            LocalDateTime lastActivityAt,
            boolean githubSyncStale,
            boolean jiraSyncStale,
            boolean hasTopic,
            double topContributorShare,
            int totalMembers) {

        // ── CRITICAL check ─────────────────────────────────────────────────
        if (isCritical(totalCommits, activeMemberRatio, overdueTasks, lastActivityAt, githubSyncStale, jiraSyncStale)) {
            return HealthStatus.CRITICAL;
        }

        // ── WARNING check ──────────────────────────────────────────────────
        int expectedThreshold = computeExpectedCommitThreshold(totalMembers);
        if (isWarning(totalCommits, activeMemberRatio, overdueTasks, topContributorShare, hasTopic, expectedThreshold)) {
            return HealthStatus.WARNING;
        }

        return HealthStatus.HEALTHY;
    }

    /**
     * Tính các lý do phụ được phát hiện (có thể nhiều lý do cùng lúc).
     * Được dùng cho danh sách {@code reasons} trong {@link GroupMonitoringMetrics}.
     *
     * @param totalCommits       tổng commit
     * @param activeMemberRatio  tỉ lệ active member
     * @param overdueTasks       số task overdue
     * @param lastActivityAt     thời điểm hoạt động gần nhất
     * @param githubSyncStale    sync GitHub stale hay không
     * @param jiraSyncStale      sync Jira stale hay không
     * @param hasTopic           có topic chưa
     * @param topContributorShare tỉ lệ top contributor
     * @param totalMembers       số thành viên
     * @return danh sách tất cả lý do được phát hiện (không theo thứ tự ưu tiên)
     */
    public List<PrimaryReason> computeAllReasons(
            long totalCommits,
            double activeMemberRatio,
            int overdueTasks,
            LocalDateTime lastActivityAt,
            boolean githubSyncStale,
            boolean jiraSyncStale,
            boolean hasTopic,
            double topContributorShare,
            int totalMembers) {

        List<PrimaryReason> reasons = new ArrayList<>();

        if (isNoActivityThisWeek(totalCommits, lastActivityAt)) {
            reasons.add(PrimaryReason.NO_ACTIVITY_THIS_WEEK);
        }
        if (isTooFewActiveMembers(activeMemberRatio)) {
            reasons.add(PrimaryReason.TOO_FEW_ACTIVE_MEMBERS);
        }
        if (isTooManyOverdueTasks(overdueTasks)) {
            reasons.add(PrimaryReason.TOO_MANY_OVERDUE_TASKS);
        }
        if (isUnevenContribution(topContributorShare, totalCommits)) {
            reasons.add(PrimaryReason.UNEVEN_CONTRIBUTION);
        }
        if (!hasTopic) {
            reasons.add(PrimaryReason.TOPIC_NOT_ASSIGNED);
        }
        if (githubSyncStale || jiraSyncStale) {
            reasons.add(PrimaryReason.STALE_SYNC);
        }

        return reasons;
    }

    /**
     * Chọn lý do chính (primary reason) theo thứ tự ưu tiên từ danh sách reasons.
     * <p>
     * Thứ tự ưu tiên:
     * NO_ACTIVITY_THIS_WEEK → TOO_FEW_ACTIVE_MEMBERS → TOO_MANY_OVERDUE_TASKS
     * → UNEVEN_CONTRIBUTION → TOPIC_NOT_ASSIGNED → STALE_SYNC → STABLE
     *
     * @param reasons danh sách lý do được phát hiện từ {@link #computeAllReasons}
     * @return lý do chính duy nhất
     */
    public PrimaryReason computePrimaryReason(List<PrimaryReason> reasons) {
        if (reasons.contains(PrimaryReason.NO_ACTIVITY_THIS_WEEK)) return PrimaryReason.NO_ACTIVITY_THIS_WEEK;
        if (reasons.contains(PrimaryReason.TOO_FEW_ACTIVE_MEMBERS))  return PrimaryReason.TOO_FEW_ACTIVE_MEMBERS;
        if (reasons.contains(PrimaryReason.TOO_MANY_OVERDUE_TASKS))  return PrimaryReason.TOO_MANY_OVERDUE_TASKS;
        if (reasons.contains(PrimaryReason.UNEVEN_CONTRIBUTION))      return PrimaryReason.UNEVEN_CONTRIBUTION;
        if (reasons.contains(PrimaryReason.TOPIC_NOT_ASSIGNED))       return PrimaryReason.TOPIC_NOT_ASSIGNED;
        if (reasons.contains(PrimaryReason.STALE_SYNC))               return PrimaryReason.STALE_SYNC;
        return PrimaryReason.STABLE;
    }

    // ────────────────────────────────────────────────────────────────────────
    // D. Class health computation
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Tính tình trạng sức khoẻ của lớp dựa trên các nhóm OPEN.
     *
     * <ul>
     *   <li>HEALTHY: không có critical group VÀ riskRatio &lt; 25%</li>
     *   <li>WARNING: đúng 1 critical group HOẶC riskRatio trong [25%, 50%)</li>
     *   <li>CRITICAL: &ge; 2 critical group HOẶC riskRatio &ge; 50%</li>
     * </ul>
     *
     * <p>Nếu không có nhóm OPEN nào → HEALTHY (không có rủi ro).
     *
     * @param totalOpenGroups tổng số nhóm OPEN
     * @param criticalGroups  số nhóm có status CRITICAL
     * @param groupsAtRisk    số nhóm có status WARNING hoặc CRITICAL
     * @return HealthStatus của lớp
     */
    public HealthStatus computeClassHealth(int totalOpenGroups, int criticalGroups, int groupsAtRisk) {
        if (totalOpenGroups == 0) {
            return HealthStatus.HEALTHY;
        }

        double riskRatio = (double) groupsAtRisk / totalOpenGroups;

        // CRITICAL: >= 2 nhóm critical HOẶC riskRatio >= 50%
        if (criticalGroups >= 2 || riskRatio >= config.getClassCriticalRiskRatio()) {
            return HealthStatus.CRITICAL;
        }

        // WARNING: đúng 1 nhóm critical HOẶC riskRatio trong [25%, 50%)
        if (criticalGroups == 1 || riskRatio >= config.getClassWarningRiskRatio()) {
            return HealthStatus.WARNING;
        }

        return HealthStatus.HEALTHY;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Ngưỡng commit kỳ vọng cho một nhóm = max(4, totalMembers * 2).
     */
    private int computeExpectedCommitThreshold(int totalMembers) {
        return Math.max(4, totalMembers * 2);
    }

    private boolean isCritical(
            long totalCommits,
            double activeMemberRatio,
            int overdueTasks,
            LocalDateTime lastActivityAt,
            boolean githubSyncStale,
            boolean jiraSyncStale) {

        if (totalCommits == 0) return true;
        if (activeMemberRatio < config.getCriticalActiveMemberRatio()) return true;
        if (overdueTasks >= config.getCriticalOverdueTasks()) return true;
        if (isLastActivityStale(lastActivityAt)) return true;
        if (githubSyncStale) return true;
        if (jiraSyncStale) return true;

        return false;
    }

    private boolean isWarning(
            long totalCommits,
            double activeMemberRatio,
            int overdueTasks,
            double topContributorShare,
            boolean hasTopic,
            int expectedThreshold) {

        if (totalCommits < expectedThreshold) return true;
        if (activeMemberRatio < config.getWarningActiveMemberRatio()) return true;
        if (overdueTasks >= config.getWarningOverdueTasksMin()) return true;
        if (isUnevenContribution(topContributorShare, totalCommits)) return true;
        if (!hasTopic) return true;

        return false;
    }

    /** Không có commit nào HOẶC lastActivity cũ hơn staleActivityDays. */
    private boolean isNoActivityThisWeek(long totalCommits, LocalDateTime lastActivityAt) {
        if (totalCommits == 0) return true;
        return isLastActivityStale(lastActivityAt);
    }

    /** lastActivity == null HOẶC cũ hơn staleActivityDays. */
    private boolean isLastActivityStale(LocalDateTime lastActivityAt) {
        if (lastActivityAt == null) return true;
        return lastActivityAt.isBefore(LocalDateTime.now().minusDays(config.getStaleActivityDays()));
    }

    private boolean isTooFewActiveMembers(double activeMemberRatio) {
        return activeMemberRatio < config.getWarningActiveMemberRatio();
    }

    private boolean isTooManyOverdueTasks(int overdueTasks) {
        return overdueTasks >= config.getWarningOverdueTasksMin();
    }

    /**
     * Đóng góp không đều nếu có ít nhất 1 commit VÀ top contributor
     * chiếm &ge; topContributorWarningRatio của tổng commit.
     */
    private boolean isUnevenContribution(double topContributorShare, long totalCommits) {
        return totalCommits > 0 && topContributorShare >= config.getTopContributorWarningRatio();
    }
}
