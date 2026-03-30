package com.swp391.backend.service.monitoring;

import com.swp391.backend.config.MonitoringConfig;
import com.swp391.backend.dto.monitoring.shared.*;
import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.entity.User;
import com.swp391.backend.entity.monitoring.ContributionStatus;
import com.swp391.backend.entity.monitoring.HealthStatus;
import com.swp391.backend.entity.monitoring.PrimaryReason;
import com.swp391.backend.repository.monitoring.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service tổng hợp dữ liệu monitoring cho nhiều nhóm/lớp cùng lúc.
 * <p>
 * Chịu trách nhiệm:
 * <ul>
 *   <li>Fetch batch data từ {@link MonitoringAggregationRepository}</li>
 *   <li>Tổng hợp thành các Map&lt;groupId, value&gt; để tránh N+1</li>
 *   <li>Gọi {@link MonitoringRuleService} để tính toán health/status</li>
 *   <li>Trả về danh sách {@link GroupMonitoringMetrics} hoặc {@link ClassMonitoringMetrics}</li>
 * </ul>
 *
 * <p>Không chứa logic business rule – tất cả rule nằm trong {@link MonitoringRuleService}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonitoringAggregationService {

    private final MonitoringAggregationRepository aggregationRepository;
    private final MonitoringRuleService ruleService;
    private final MonitoringConfig config;

    /**
     * Tính toán {@link GroupMonitoringMetrics} cho danh sách nhóm trong khoảng thời gian.
     * <p>
     * Sử dụng batch queries để tránh N+1. Tất cả dữ liệu được fetch trong
     * tối đa ~6 queries bất kể số lượng nhóm.
     *
     * @param groups    danh sách StudentGroup cần tính (nên là OPEN groups)
     * @param dateRange khoảng thời gian đã resolve
     * @return danh sách GroupMonitoringMetrics theo thứ tự tương ứng với groups
     */
    public List<GroupMonitoringMetrics> computeGroupMetrics(
            List<StudentGroup> groups,
            MonitoringDateRange dateRange) {

        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> groupIds = groups.stream()
                .map(StudentGroup::getGroupId)
                .collect(Collectors.toList());

        LocalDateTime from = dateRange.getFrom();
        LocalDateTime to = dateRange.getTo();
        LocalDateTime now = LocalDateTime.now();

        // ── Batch fetch tất cả dữ liệu cần thiết ─────────────────────────

        // 1. Tổng thành viên
        Map<Long, Long> memberCountMap = toGroupLongMap(
                aggregationRepository.countMembersByGroupIds(groupIds),
                GroupMemberCountProjection::getGroupId,
                GroupMemberCountProjection::getTotalMembers);

        // 2. Thành viên active
        Map<Long, Long> activeMemberMap = toGroupLongMap(
                aggregationRepository.countActiveMembersByGroupIds(groupIds, from, to),
                GroupActiveMemberCountProjection::getGroupId,
                GroupActiveMemberCountProjection::getActiveMembers);

        // 3. Commit summary (tổng commit + thời điểm gần nhất)
        Map<Long, GroupCommitSummaryProjection> commitSummaryMap =
                aggregationRepository.getCommitSummaryByGroupIds(groupIds, from, to)
                        .stream()
                        .collect(Collectors.toMap(GroupCommitSummaryProjection::getGroupId, p -> p));

        // 4. Commit theo user (để tính top contributor share)
        Map<Long, List<UserGroupCommitProjection>> userCommitsByGroup =
                aggregationRepository.getUserCommitsByGroupIds(groupIds, from, to)
                        .stream()
                        .collect(Collectors.groupingBy(UserGroupCommitProjection::getGroupId));

        // 5. Overdue tasks
        Map<Long, Long> overdueTaskMap = toGroupLongMap(
                aggregationRepository.countOverdueTasksByGroupIds(groupIds, now),
                GroupOverdueTaskCountProjection::getGroupId,
                GroupOverdueTaskCountProjection::getOverdueTasks);

        // 6. Sync status
        Map<Long, Map<String, LocalDateTime>> syncMap =
                buildSyncMap(aggregationRepository.getLastSuccessfulSyncByGroupIds(groupIds));

        // 7. Topic existence
        Set<Long> groupsWithTopic = new HashSet<>(
                aggregationRepository.findGroupIdsWithTopic(groupIds));

        // ── Build metrics per group ───────────────────────────────────────

        return groups.stream()
                .map(group -> buildGroupMetrics(
                        group, memberCountMap, activeMemberMap, commitSummaryMap,
                        userCommitsByGroup, overdueTaskMap, syncMap, groupsWithTopic, from, to))
                .collect(Collectors.toList());
    }

    /**
     * Tính toán {@link ClassMonitoringMetrics} từ danh sách GroupMonitoringMetrics của lớp.
     * <p>
     * Chỉ tính trên OPEN groups.
     *
     * @param classId      ID lớp học
     * @param classCode    mã lớp
     * @param groupMetrics danh sách metrics của các nhóm (đã tính xong)
     * @return ClassMonitoringMetrics
     */
    public ClassMonitoringMetrics computeClassMetrics(
            Long classId,
            String classCode,
            List<GroupMonitoringMetrics> groupMetrics) {

        int totalOpenGroups = groupMetrics.size();

        int criticalGroups = 0;
        int groupsAtRisk = 0;
        int studentsFlagged = 0;

        for (GroupMonitoringMetrics gm : groupMetrics) {
            if (gm.getHealthStatus() == HealthStatus.CRITICAL) {
                criticalGroups++;
                groupsAtRisk++;
            } else if (gm.getHealthStatus() == HealthStatus.WARNING) {
                groupsAtRisk++;
            }
            // flagged = thành viên không ACTIVE = totalMembers - activeMembers
            studentsFlagged += (gm.getTotalMembers() - gm.getActiveMembers());
        }

        HealthStatus classHealth = ruleService.computeClassHealth(totalOpenGroups, criticalGroups, groupsAtRisk);

        return ClassMonitoringMetrics.builder()
                .classId(classId)
                .classCode(classCode)
                .totalOpenGroups(totalOpenGroups)
                .groupsAtRisk(groupsAtRisk)
                .criticalGroups(criticalGroups)
                .studentsFlagged(studentsFlagged)
                .classHealth(classHealth)
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Tính toán {@link StudentContributionMetrics} từ user commit data trong nhóm.
     * <p>
     * Dùng cho BE-04 (Students Watchlist) và BE-05 (Group Monitoring Detail).
     *
     * @param members     danh sách User trong nhóm
     * @param groupId     ID nhóm
     * @param groupName   tên nhóm
     * @param dateRange   khoảng thời gian
     * @return danh sách StudentContributionMetrics
     */
    public List<StudentContributionMetrics> computeStudentMetrics(
            List<User> members,
            Long groupId,
            String groupName,
            MonitoringDateRange dateRange) {

        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> groupIds = List.of(groupId);
        LocalDateTime from = dateRange.getFrom();
        LocalDateTime to = dateRange.getTo();

        // Fetch commit data for this single group (still batch-friendly pattern)
        Map<Long, UserGroupCommitProjection> userCommitMap =
                aggregationRepository.getUserCommitsByGroupIds(groupIds, from, to)
                        .stream()
                        .filter(p -> p.getGroupId().equals(groupId))
                        .filter(p -> p.getUserId() != null)
                        .collect(Collectors.toMap(UserGroupCommitProjection::getUserId, p -> p, (a, b) -> a));

        return members.stream()
                .map(user -> {
                    UserGroupCommitProjection commit = userCommitMap.get(user.getUserId());
                    long commitCount = commit != null ? commit.getCommitCount() : 0L;
                    LocalDateTime lastActive = commit != null ? commit.getLastCommitAt() : null;
                    ContributionStatus status = ruleService.computeContributionStatus(commitCount);

                    return StudentContributionMetrics.builder()
                            .userId(user.getUserId())
                            .fullName(user.getFullName())
                            .studentCode(user.getUsername())
                            .groupId(groupId)
                            .groupName(groupName)
                            .commitCount(commitCount)
                            .lastActiveAt(lastActive)
                            .contributionStatus(status)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────────────────

    private GroupMonitoringMetrics buildGroupMetrics(
            StudentGroup group,
            Map<Long, Long> memberCountMap,
            Map<Long, Long> activeMemberMap,
            Map<Long, GroupCommitSummaryProjection> commitSummaryMap,
            Map<Long, List<UserGroupCommitProjection>> userCommitsByGroup,
            Map<Long, Long> overdueTaskMap,
            Map<Long, Map<String, LocalDateTime>> syncMap,
            Set<Long> groupsWithTopic,
            LocalDateTime from,
            LocalDateTime to) {

        Long groupId = group.getGroupId();

        int totalMembers = memberCountMap.getOrDefault(groupId, 0L).intValue();
        int activeMembers = activeMemberMap.getOrDefault(groupId, 0L).intValue();
        double activeMemberRatio = totalMembers > 0 ? (double) activeMembers / totalMembers : 0.0;

        GroupCommitSummaryProjection commitSummary = commitSummaryMap.get(groupId);
        long totalCommits = commitSummary != null ? commitSummary.getTotalCommits() : 0L;
        LocalDateTime lastActivityAt = commitSummary != null ? commitSummary.getLastCommitAt() : null;

        int overdueTasks = overdueTaskMap.getOrDefault(groupId, 0L).intValue();

        // Top contributor share
        double topContributorShare = computeTopContributorShare(
                userCommitsByGroup.getOrDefault(groupId, Collections.emptyList()), totalCommits);

        // Sync staleness
        Map<String, LocalDateTime> groupSync = syncMap.getOrDefault(groupId, Collections.emptyMap());
        boolean githubSyncStale = isSyncStale(groupSync.get("GITHUB"));
        boolean jiraSyncStale = isSyncStale(groupSync.get("JIRA"));

        boolean hasTopic = groupsWithTopic.contains(groupId);

        // Topic name
        String topicName = group.getTopic() != null ? group.getTopic().getTopicName() : null;

        // Compute health
        HealthStatus healthStatus = ruleService.computeGroupHealth(
                totalCommits, activeMemberRatio, overdueTasks, lastActivityAt,
                githubSyncStale, jiraSyncStale, hasTopic, topContributorShare, totalMembers);

        // Compute reasons
        List<PrimaryReason> reasons = ruleService.computeAllReasons(
                totalCommits, activeMemberRatio, overdueTasks, lastActivityAt,
                githubSyncStale, jiraSyncStale, hasTopic, topContributorShare, totalMembers);

        PrimaryReason primaryReason = ruleService.computePrimaryReason(reasons);

        return GroupMonitoringMetrics.builder()
                .groupId(groupId)
                .groupName(group.getGroupName())
                .classId(group.getAcademicClass() != null ? group.getAcademicClass().getClassId() : null)
                .topicName(topicName)
                .operationalStatus(group.getStatus())
                .totalMembers(totalMembers)
                .activeMembers(activeMembers)
                .activeMemberRatio(activeMemberRatio)
                .totalCommits(totalCommits)
                .overdueTasks(overdueTasks)
                .topContributorShare(topContributorShare)
                .lastActivityAt(lastActivityAt)
                .hasTopic(hasTopic)
                .githubSyncStale(githubSyncStale)
                .jiraSyncStale(jiraSyncStale)
                .healthStatus(healthStatus)
                .primaryReason(primaryReason)
                .reasons(reasons)
                .build();
    }

    /**
     * Tính tỉ lệ commit của top contributor.
     * Trả về 0.0 nếu không có commit.
     */
    private double computeTopContributorShare(List<UserGroupCommitProjection> userCommits, long totalCommits) {
        if (totalCommits == 0 || userCommits.isEmpty()) {
            return 0.0;
        }
        long maxCommit = userCommits.stream()
                .mapToLong(UserGroupCommitProjection::getCommitCount)
                .max()
                .orElse(0L);
        return (double) maxCommit / totalCommits;
    }

    /**
     * Kiểm tra sync có bị stale không.
     * Stale nếu lastSyncAt == null HOẶC cũ hơn staleActivityDays.
     */
    private boolean isSyncStale(LocalDateTime lastSyncAt) {
        if (lastSyncAt == null) return true;
        return lastSyncAt.isBefore(LocalDateTime.now().minusDays(config.getStaleActivityDays()));
    }

    /**
     * Build sync map: groupId → {source → lastSyncAt}.
     */
    private Map<Long, Map<String, LocalDateTime>> buildSyncMap(List<GroupSyncStatusProjection> projections) {
        Map<Long, Map<String, LocalDateTime>> result = new HashMap<>();
        for (GroupSyncStatusProjection p : projections) {
            result
                    .computeIfAbsent(p.getGroupId(), k -> new HashMap<>())
                    .put(p.getSource(), p.getLastSyncAt());
        }
        return result;
    }

    /**
     * Helper để convert List&lt;P&gt; thành Map&lt;Long, Long&gt; theo groupId.
     */
    private <P> Map<Long, Long> toGroupLongMap(
            List<P> projections,
            java.util.function.Function<P, Long> keyExtractor,
            java.util.function.Function<P, Long> valueExtractor) {
        return projections.stream()
                .collect(Collectors.toMap(keyExtractor, valueExtractor));
    }
}
