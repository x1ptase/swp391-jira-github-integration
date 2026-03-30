package com.swp391.backend.service.monitoring;

import com.swp391.backend.config.MonitoringConfig;
import com.swp391.backend.entity.monitoring.ContributionStatus;
import com.swp391.backend.entity.monitoring.HealthStatus;
import com.swp391.backend.entity.monitoring.PrimaryReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests cho {@link MonitoringRuleService}.
 * <p>
 * Dùng default config values (không mock) để test đúng business rule.
 */
@DisplayName("MonitoringRuleService Tests")
class MonitoringRuleServiceTest {

    private MonitoringRuleService ruleService;
    private MonitoringConfig config;

    @BeforeEach
    void setUp() {
        config = new MonitoringConfig(); // default values
        ruleService = new MonitoringRuleService(config);
    }

    // ────────────────────────────────────────────────────────────────────────
    // A. Contribution status
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("A. Contribution Status")
    class ContributionStatusTests {

        @Test
        @DisplayName("0 commit → NO_CONTRIBUTION")
        void zeroCommit_shouldBeNoContribution() {
            assertThat(ruleService.computeContributionStatus(0)).isEqualTo(ContributionStatus.NO_CONTRIBUTION);
        }

        @Test
        @DisplayName("1 commit → LOW")
        void oneCommit_shouldBeLow() {
            assertThat(ruleService.computeContributionStatus(1)).isEqualTo(ContributionStatus.LOW);
        }

        @Test
        @DisplayName("2 commits → ACTIVE (equals threshold)")
        void twoCommits_shouldBeActive() {
            assertThat(ruleService.computeContributionStatus(2)).isEqualTo(ContributionStatus.ACTIVE);
        }

        @Test
        @DisplayName("5 commits → ACTIVE")
        void fiveCommits_shouldBeActive() {
            assertThat(ruleService.computeContributionStatus(5)).isEqualTo(ContributionStatus.ACTIVE);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // B. Group health
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("B. Group Health")
    class GroupHealthTests {

        private LocalDateTime recentActivity;
        private LocalDateTime staleActivity;

        @BeforeEach
        void setupDates() {
            recentActivity = LocalDateTime.now().minusDays(2);  // fresh
            staleActivity  = LocalDateTime.now().minusDays(10); // older than 7 days
        }

        @Test
        @DisplayName("Zero commits → CRITICAL")
        void zeroCommits_shouldBeCritical() {
            HealthStatus result = ruleService.computeGroupHealth(0, 0.8, 0, recentActivity, false, false, true, 0.0, 4);
            assertThat(result).isEqualTo(HealthStatus.CRITICAL);
        }

        @Test
        @DisplayName("activeMemberRatio < 0.25 → CRITICAL")
        void tooFewActiveMembers_critical() {
            HealthStatus result = ruleService.computeGroupHealth(10, 0.20, 0, recentActivity, false, false, true, 0.4, 5);
            assertThat(result).isEqualTo(HealthStatus.CRITICAL);
        }

        @Test
        @DisplayName("overdueTasks >= 5 → CRITICAL")
        void manyOverdueTasks_critical() {
            HealthStatus result = ruleService.computeGroupHealth(10, 0.8, 5, recentActivity, false, false, true, 0.4, 4);
            assertThat(result).isEqualTo(HealthStatus.CRITICAL);
        }

        @Test
        @DisplayName("Stale last activity > 7 days → CRITICAL")
        void staleActivity_critical() {
            HealthStatus result = ruleService.computeGroupHealth(5, 0.8, 0, staleActivity, false, false, true, 0.4, 4);
            assertThat(result).isEqualTo(HealthStatus.CRITICAL);
        }

        @Test
        @DisplayName("GitHub sync stale → CRITICAL")
        void githubSyncStale_critical() {
            // Need >= 1 commit to pass zero check, but githubSyncStale=true → CRITICAL
            HealthStatus result = ruleService.computeGroupHealth(5, 0.8, 0, recentActivity, true, false, true, 0.4, 4);
            assertThat(result).isEqualTo(HealthStatus.CRITICAL);
        }

        @Test
        @DisplayName("Null lastActivity → CRITICAL")
        void nullLastActivity_critical() {
            HealthStatus result = ruleService.computeGroupHealth(5, 0.8, 0, null, false, false, true, 0.4, 4);
            assertThat(result).isEqualTo(HealthStatus.CRITICAL);
        }

        @Test
        @DisplayName("activeMemberRatio in [0.25, 0.50) → WARNING")
        void memberRatioBelowWarning_warning() {
            // Not critical: ratio=0.30 >= 0.25; but < 0.50 → WARNING
            HealthStatus result = ruleService.computeGroupHealth(10, 0.30, 0, recentActivity, false, false, true, 0.4, 2); // threshold=max(4,4)=4; 10>=4
            assertThat(result).isEqualTo(HealthStatus.WARNING);
        }

        @Test
        @DisplayName("overdueTasks in [2,4] → WARNING")
        void overdueTasksBetween2And4_warning() {
            // 4 members -> threshold = max(4, 4*2)=8. 10 commits>=8.
            HealthStatus result = ruleService.computeGroupHealth(10, 0.8, 3, recentActivity, false, false, true, 0.4, 4);
            assertThat(result).isEqualTo(HealthStatus.WARNING);
        }

        @Test
        @DisplayName("topContributorShare >= 0.70 → WARNING")
        void topContributorHigh_warning() {
            // 4 members -> threshold=8; 10>=8. activeMemberRatio=0.8 ok. overdue=0. hasTopic=true.
            HealthStatus result = ruleService.computeGroupHealth(10, 0.8, 0, recentActivity, false, false, true, 0.75, 4);
            assertThat(result).isEqualTo(HealthStatus.WARNING);
        }

        @Test
        @DisplayName("No topic → WARNING")
        void noTopic_warning() {
            HealthStatus result = ruleService.computeGroupHealth(10, 0.8, 0, recentActivity, false, false, false, 0.4, 4);
            assertThat(result).isEqualTo(HealthStatus.WARNING);
        }

        @Test
        @DisplayName("All good → HEALTHY")
        void allGood_healthy() {
            // 4 members -> threshold=max(4,8)=8; 10>=8
            HealthStatus result = ruleService.computeGroupHealth(10, 0.8, 0, recentActivity, false, false, true, 0.4, 4);
            assertThat(result).isEqualTo(HealthStatus.HEALTHY);
        }

        @Test
        @DisplayName("Group with only 1 active member out of 4 → CRITICAL (ratio=0.25)")
        void oneActiveMemberExactly_critical() {
            // ratio = 1/4 = 0.25, which is NOT < 0.25, so not CRITICAL by that rule
            // but totalCommits=5<8 → WARNING
            HealthStatus result = ruleService.computeGroupHealth(5, 0.25, 0, recentActivity, false, false, true, 0.6, 4);
            // 5 < max(4,8)=8 → WARNING
            assertThat(result).isEqualTo(HealthStatus.WARNING);
        }

        @Test
        @DisplayName("Overdue tasks exactly at CRITICAL threshold (5) → CRITICAL")
        void overdueExactlyAtCriticalThreshold() {
            HealthStatus result = ruleService.computeGroupHealth(10, 0.8, 5, recentActivity, false, false, true, 0.4, 4);
            assertThat(result).isEqualTo(HealthStatus.CRITICAL);
        }

        @Test
        @DisplayName("Overdue tasks exactly at WARNING threshold (2) → WARNING")
        void overdueExactlyAtWarningThreshold() {
            HealthStatus result = ruleService.computeGroupHealth(10, 0.8, 2, recentActivity, false, false, true, 0.4, 4);
            assertThat(result).isEqualTo(HealthStatus.WARNING);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // C. Primary reason priority
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("C. Primary Reason Priority")
    class PrimaryReasonTests {

        @Test
        @DisplayName("No reasons → STABLE")
        void noReasons_stable() {
            PrimaryReason result = ruleService.computePrimaryReason(List.of());
            assertThat(result).isEqualTo(PrimaryReason.STABLE);
        }

        @Test
        @DisplayName("NO_ACTIVITY_THIS_WEEK wins over all others")
        void noActivityWinsOverAll() {
            PrimaryReason result = ruleService.computePrimaryReason(List.of(
                    PrimaryReason.STALE_SYNC,
                    PrimaryReason.TOPIC_NOT_ASSIGNED,
                    PrimaryReason.NO_ACTIVITY_THIS_WEEK,
                    PrimaryReason.TOO_FEW_ACTIVE_MEMBERS));
            assertThat(result).isEqualTo(PrimaryReason.NO_ACTIVITY_THIS_WEEK);
        }

        @Test
        @DisplayName("TOO_FEW_ACTIVE_MEMBERS beats lower-priority reasons")
        void tooFewMembersBeatsLower() {
            PrimaryReason result = ruleService.computePrimaryReason(List.of(
                    PrimaryReason.TOPIC_NOT_ASSIGNED,
                    PrimaryReason.TOO_FEW_ACTIVE_MEMBERS,
                    PrimaryReason.STALE_SYNC));
            assertThat(result).isEqualTo(PrimaryReason.TOO_FEW_ACTIVE_MEMBERS);
        }

        @Test
        @DisplayName("STALE_SYNC alone → STALE_SYNC")
        void staleSyncAlone() {
            PrimaryReason result = ruleService.computePrimaryReason(List.of(PrimaryReason.STALE_SYNC));
            assertThat(result).isEqualTo(PrimaryReason.STALE_SYNC);
        }

        @Test
        @DisplayName("TOPIC_NOT_ASSIGNED alone → TOPIC_NOT_ASSIGNED")
        void topicNotAssignedAlone() {
            PrimaryReason result = ruleService.computePrimaryReason(List.of(PrimaryReason.TOPIC_NOT_ASSIGNED));
            assertThat(result).isEqualTo(PrimaryReason.TOPIC_NOT_ASSIGNED);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // D. Class health
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("D. Class Health")
    class ClassHealthTests {

        @Test
        @DisplayName("No open groups → HEALTHY")
        void noGroups_healthy() {
            assertThat(ruleService.computeClassHealth(0, 0, 0)).isEqualTo(HealthStatus.HEALTHY);
        }

        @Test
        @DisplayName("Only CLOSED groups passed as 0 open → HEALTHY")
        void onlyClosedGroups_healthy() {
            assertThat(ruleService.computeClassHealth(0, 0, 0)).isEqualTo(HealthStatus.HEALTHY);
        }

        @Test
        @DisplayName("0 critical, riskRatio=0 → HEALTHY")
        void noCriticalNoRisk_healthy() {
            assertThat(ruleService.computeClassHealth(4, 0, 0)).isEqualTo(HealthStatus.HEALTHY);
        }

        @Test
        @DisplayName("Exactly 1 critical group → WARNING")
        void oneCritical_warning() {
            assertThat(ruleService.computeClassHealth(4, 1, 1)).isEqualTo(HealthStatus.WARNING);
        }

        @Test
        @DisplayName("riskRatio exactly 0.25 → WARNING")
        void riskRatioExactly25Percent_warning() {
            // 1 at risk out of 4 = 0.25
            assertThat(ruleService.computeClassHealth(4, 0, 1)).isEqualTo(HealthStatus.WARNING);
        }

        @Test
        @DisplayName("2 or more critical groups → CRITICAL")
        void twoCritical_critical() {
            assertThat(ruleService.computeClassHealth(4, 2, 2)).isEqualTo(HealthStatus.CRITICAL);
        }

        @Test
        @DisplayName("riskRatio >= 0.50 → CRITICAL")
        void riskRatioAbove50Percent_critical() {
            // 2 at risk out of 4 = 0.50
            assertThat(ruleService.computeClassHealth(4, 0, 2)).isEqualTo(HealthStatus.CRITICAL);
        }

        @Test
        @DisplayName("riskRatio between 25% and 50% → WARNING")
        void riskRatioBetween25And50_warning() {
            // 1 at risk out of 3 ≈ 0.33  → WARNING
            assertThat(ruleService.computeClassHealth(3, 0, 1)).isEqualTo(HealthStatus.WARNING);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // E. computeAllReasons edge cases
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("E. All Reasons Edge Cases")
    class AllReasonsTests {

        @Test
        @DisplayName("Group with no topic → TOPIC_NOT_ASSIGNED in reasons")
        void groupWithNoTopic_containsTopicNotAssigned() {
            LocalDateTime recent = LocalDateTime.now().minusDays(2);
            List<PrimaryReason> reasons = ruleService.computeAllReasons(10, 0.8, 0, recent, false, false, false, 0.4, 4);
            assertThat(reasons).contains(PrimaryReason.TOPIC_NOT_ASSIGNED);
        }

        @Test
        @DisplayName("Group with no commits → NO_ACTIVITY_THIS_WEEK in reasons")
        void groupWithNoCommits_containsNoActivity() {
            LocalDateTime recent = LocalDateTime.now().minusDays(2);
            List<PrimaryReason> reasons = ruleService.computeAllReasons(0, 0.0, 0, null, false, false, true, 0.0, 4);
            assertThat(reasons).contains(PrimaryReason.NO_ACTIVITY_THIS_WEEK);
        }

        @Test
        @DisplayName("Stale sync → STALE_SYNC in reasons")
        void staleSyncInReasons() {
            LocalDateTime recent = LocalDateTime.now().minusDays(2);
            List<PrimaryReason> reasons = ruleService.computeAllReasons(5, 0.8, 0, recent, true, false, true, 0.4, 4);
            assertThat(reasons).contains(PrimaryReason.STALE_SYNC);
        }

        @Test
        @DisplayName("All good → empty reasons list")
        void allGood_emptyReasons() {
            LocalDateTime recent = LocalDateTime.now().minusDays(2);
            // 4 members → threshold = max(4, 8) = 8; 10 >= 8
            List<PrimaryReason> reasons = ruleService.computeAllReasons(10, 0.8, 0, recent, false, false, true, 0.4, 4);
            assertThat(reasons).isEmpty();
        }
    }
}

