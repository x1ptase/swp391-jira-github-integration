package com.swp391.backend.entity.monitoring;

/**
 * Mức độ đóng góp của một sinh viên trong khoảng thời gian monitoring.
 * <p>
 * Được tính dựa trên số commit trong cửa sổ thời gian đang theo dõi.
 * Ngưỡng cụ thể được cấu hình trong {@link com.swp391.backend.config.MonitoringConfig}.
 */
public enum ContributionStatus {

    /**
     * Sinh viên có commit &gt;= ngưỡng ACTIVE (mặc định: 2).
     */
    ACTIVE,

    /**
     * Sinh viên có đúng 1 commit trong khoảng thời gian.
     */
    LOW,

    /**
     * Sinh viên không có commit nào trong khoảng thời gian.
     */
    NO_CONTRIBUTION
}
