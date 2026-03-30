package com.swp391.backend.dto.monitoring.shared;

import com.swp391.backend.entity.monitoring.ContributionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DTO nội bộ chứa metrics đóng góp của một sinh viên trong khoảng monitoring.
 * <p>
 * Không phải API response trực tiếp – được tái sử dụng bởi các service-layer
 * như BE-04 (Students Watchlist) và BE-05 (Group Monitoring Detail).
 */
@Getter
@Builder
public class StudentContributionMetrics {

    /** ID người dùng nội bộ. */
    private final Long userId;

    /** Họ tên đầy đủ. */
    private final String fullName;

    /** Mã số sinh viên. */
    private final String studentCode;

    /** ID nhóm mà sinh viên thuộc về. */
    private final Long groupId;

    /** Tên nhóm. */
    private final String groupName;

    /** Số commit trong khoảng thời gian monitoring. */
    private final long commitCount;

    /** Thời điểm commit gần nhất; null nếu chưa có commit. */
    private final LocalDateTime lastActiveAt;

    /** Mức đóng góp được tính từ commitCount. */
    private final ContributionStatus contributionStatus;
}
