package com.swp391.backend.dto.monitoring.shared;

import com.swp391.backend.entity.monitoring.HealthStatus;
import com.swp391.backend.entity.monitoring.PrimaryReason;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO nội bộ chứa metrics tổng hợp của một nhóm sinh viên trong khoảng monitoring.
 * <p>
 * Không phải API response trực tiếp – tái sử dụng bởi BE-02, BE-03, BE-05.
 * {@code operationalStatus} phản ánh {@code StudentGroup.status} (OPEN/CLOSED);
 * {@code healthStatus} là giá trị tính toán động, không lưu vào DB.
 */
@Getter
@Builder
public class GroupMonitoringMetrics {

    /** ID nhóm. */
    private final Long groupId;

    /** Tên nhóm. */
    private final String groupName;

    /** ID lớp học. */
    private final Long classId;

    /** Tên đề tài (nếu đã gán); null nếu chưa có. */
    private final String topicName;

    /**
     * Trạng thái vận hành từ DB: OPEN hoặc CLOSED.
     * Không phải health status.
     */
    private final String operationalStatus;

    /** Tổng số thành viên trong nhóm. */
    private final int totalMembers;

    /** Số thành viên có commit trong khoảng thời gian monitoring. */
    private final int activeMembers;

    /**
     * Tỉ lệ thành viên active = activeMembers / totalMembers.
     * 0.0 nếu totalMembers = 0.
     */
    private final double activeMemberRatio;

    /** Tổng số commit của cả nhóm trong khoảng thời gian. */
    private final long totalCommits;

    /** Số task quá hạn (due_date &lt; now và chưa DONE). */
    private final int overdueTasks;

    /**
     * Tỉ lệ commit của thành viên đóng góp nhiều nhất / tổng commit nhóm.
     * 0.0 nếu totalCommits = 0.
     */
    private final double topContributorShare;

    /** Thời điểm commit gần nhất của cả nhóm; null nếu chưa có commit. */
    private final LocalDateTime lastActivityAt;

    /** Nhóm đã được gán topic hay chưa. */
    private final boolean hasTopic;

    /**
     * Dữ liệu GitHub sync có bị stale không
     * (lastSync &gt; STALE_ACTIVITY_DAYS ngày trước hoặc chưa sync).
     */
    private final boolean githubSyncStale;

    /**
     * Dữ liệu Jira sync có bị stale không
     * (lastSync &gt; STALE_ACTIVITY_DAYS ngày trước hoặc chưa sync).
     */
    private final boolean jiraSyncStale;

    /** Tình trạng sức khoẻ tổng hợp được tính toán. */
    private final HealthStatus healthStatus;

    /** Lý do chính đầu tiên (theo priority). */
    private final PrimaryReason primaryReason;

    /**
     * Tất cả lý do phụ được phát hiện (có thể nhiều lý do cùng lúc).
     * Dùng để hiển thị chi tiết hơn trong API response.
     */
    private final List<PrimaryReason> reasons;
}
