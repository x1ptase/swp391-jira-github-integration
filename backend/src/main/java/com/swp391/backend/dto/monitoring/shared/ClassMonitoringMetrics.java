package com.swp391.backend.dto.monitoring.shared;

import com.swp391.backend.entity.monitoring.HealthStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DTO nội bộ chứa metrics tổng hợp tình trạng một lớp học.
 * <p>
 * Tái sử dụng bởi BE-01 (Lecturer Classes Monitoring) và BE-02 (Class Monitoring Summary).
 * <p>
 * Chỉ tính trên các nhóm có {@code operationalStatus = OPEN}.
 * Nhóm CLOSED không được đưa vào tính toán risk ratio.
 */
@Getter
@Builder
public class ClassMonitoringMetrics {

    /** ID lớp học. */
    private final Long classId;

    /** Mã lớp học (ví dụ: SE1716). */
    private final String classCode;

    /** Tổng số nhóm OPEN. */
    private final int totalOpenGroups;

    /** Số nhóm có healthStatus = CRITICAL hoặc WARNING. */
    private final int groupsAtRisk;

    /** Số nhóm có healthStatus = CRITICAL. */
    private final int criticalGroups;

    /** Tổng số sinh viên bị đánh dấu (ContributionStatus != ACTIVE). */
    private final int studentsFlagged;

    /** Tình trạng sức khoẻ tổng hợp của lớp. */
    private final HealthStatus classHealth;

    /** Thời điểm tính toán (now tại thời điểm gọi service). */
    private final LocalDateTime lastUpdatedAt;
}
