package com.swp391.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình tập trung cho module monitoring.
 * <p>
 * Các ngưỡng (threshold) được khai báo tại đây, có thể override
 * qua {@code application.properties} hoặc biến môi trường với prefix
 * {@code monitoring}.
 *
 * <p><b>Ví dụ override:</b>
 * <pre>
 * monitoring.active-commit-threshold=3
 * monitoring.stale-activity-days=14
 * </pre>
 *
 * <p><b>Timezone assumption:</b> Tất cả tính toán ngày tháng dùng
 * {@code LocalDateTime} (server timezone). Không có conversion timezone
 * tự động – nên đảm bảo server chạy đúng timezone của dự án
 * (khuyến nghị: Asia/Ho_Chi_Minh hoặc UTC).
 */
@Configuration
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringConfig {

    // ── Commit thresholds ─────────────────────────────────────────────────────

    /**
     * Số commit tối thiểu để một sinh viên được coi là ACTIVE.
     * Mặc định: 2.
     */
    private int activeCommitThreshold = 2;

    /**
     * Số commit tối thiểu để một sinh viên được coi là LOW (không phải NO_CONTRIBUTION).
     * Mặc định: 1.
     */
    private int lowCommitThreshold = 1;

    // ── Member ratio thresholds ───────────────────────────────────────────────

    /**
     * Tỉ lệ thành viên active tối thiểu; thấp hơn → CRITICAL.
     * Mặc định: 0.25 (25%).
     */
    private double criticalActiveMemberRatio = 0.25;

    /**
     * Tỉ lệ thành viên active tối thiểu; thấp hơn (nhưng ≥ criticalActiveMemberRatio) → WARNING.
     * Mặc định: 0.50 (50%).
     */
    private double warningActiveMemberRatio = 0.50;

    // ── Overdue task thresholds ───────────────────────────────────────────────

    /**
     * Số task overdue để trigger CRITICAL.
     * Mặc định: 5.
     */
    private int criticalOverdueTasks = 5;

    /**
     * Số task overdue tối thiểu để trigger WARNING.
     * Mặc định: 2.
     */
    private int warningOverdueTasksMin = 2;

    // ── Contribution evenness ─────────────────────────────────────────────────

    /**
     * Nếu một thành viên chiếm &gt;= tỉ lệ này trong tổng commit → WARNING (phân bổ không đều).
     * Mặc định: 0.70 (70%).
     */
    private double topContributorWarningRatio = 0.70;

    // ── Staleness ─────────────────────────────────────────────────────────────

    /**
     * Số ngày không có hoạt động (commit) để coi là STALE.
     * Mặc định: 7.
     */
    private int staleActivityDays = 7;

    // ── Default monitoring window ─────────────────────────────────────────────

    /**
     * Khoảng thời gian mặc định (ngày) nếu không có fromDate/toDate/lastNDays.
     * Mặc định: 7 (một tuần).
     */
    private int defaultMonitoringWindowDays = 7;

    // ── Class health thresholds ───────────────────────────────────────────────

    /**
     * Tỉ lệ nhóm có rủi ro (risk ratio) để trigger WARNING ở cấp lớp.
     * Mặc định: 0.25 (25%).
     */
    private double classWarningRiskRatio = 0.25;

    /**
     * Tỉ lệ nhóm có rủi ro để trigger CRITICAL ở cấp lớp.
     * Mặc định: 0.50 (50%).
     */
    private double classCriticalRiskRatio = 0.50;

    // ── Getters and setters ───────────────────────────────────────────────────

    public int getActiveCommitThreshold() {
        return activeCommitThreshold;
    }

    public void setActiveCommitThreshold(int activeCommitThreshold) {
        this.activeCommitThreshold = activeCommitThreshold;
    }

    public int getLowCommitThreshold() {
        return lowCommitThreshold;
    }

    public void setLowCommitThreshold(int lowCommitThreshold) {
        this.lowCommitThreshold = lowCommitThreshold;
    }

    public double getCriticalActiveMemberRatio() {
        return criticalActiveMemberRatio;
    }

    public void setCriticalActiveMemberRatio(double criticalActiveMemberRatio) {
        this.criticalActiveMemberRatio = criticalActiveMemberRatio;
    }

    public double getWarningActiveMemberRatio() {
        return warningActiveMemberRatio;
    }

    public void setWarningActiveMemberRatio(double warningActiveMemberRatio) {
        this.warningActiveMemberRatio = warningActiveMemberRatio;
    }

    public int getCriticalOverdueTasks() {
        return criticalOverdueTasks;
    }

    public void setCriticalOverdueTasks(int criticalOverdueTasks) {
        this.criticalOverdueTasks = criticalOverdueTasks;
    }

    public int getWarningOverdueTasksMin() {
        return warningOverdueTasksMin;
    }

    public void setWarningOverdueTasksMin(int warningOverdueTasksMin) {
        this.warningOverdueTasksMin = warningOverdueTasksMin;
    }

    public double getTopContributorWarningRatio() {
        return topContributorWarningRatio;
    }

    public void setTopContributorWarningRatio(double topContributorWarningRatio) {
        this.topContributorWarningRatio = topContributorWarningRatio;
    }

    public int getStaleActivityDays() {
        return staleActivityDays;
    }

    public void setStaleActivityDays(int staleActivityDays) {
        this.staleActivityDays = staleActivityDays;
    }

    public int getDefaultMonitoringWindowDays() {
        return defaultMonitoringWindowDays;
    }

    public void setDefaultMonitoringWindowDays(int defaultMonitoringWindowDays) {
        this.defaultMonitoringWindowDays = defaultMonitoringWindowDays;
    }

    public double getClassWarningRiskRatio() {
        return classWarningRiskRatio;
    }

    public void setClassWarningRiskRatio(double classWarningRiskRatio) {
        this.classWarningRiskRatio = classWarningRiskRatio;
    }

    public double getClassCriticalRiskRatio() {
        return classCriticalRiskRatio;
    }

    public void setClassCriticalRiskRatio(double classCriticalRiskRatio) {
        this.classCriticalRiskRatio = classCriticalRiskRatio;
    }
}
