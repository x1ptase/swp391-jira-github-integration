package com.swp391.backend.entity.monitoring;

/**
 * Lý do chính (primary reason) được chọn khi tổng hợp tình trạng nhóm.
 * <p>
 * Nếu nhiều điều kiện cùng xảy ra, chỉ chọn đúng một lý do theo
 * thứ tự ưu tiên được định nghĩa trong
 * {@link com.swp391.backend.service.monitoring.MonitoringRuleService}.
 *
 * <h4>Thứ tự ưu tiên (cao → thấp):</h4>
 * <ol>
 *   <li>{@link #NO_ACTIVITY_THIS_WEEK}</li>
 *   <li>{@link #TOO_FEW_ACTIVE_MEMBERS}</li>
 *   <li>{@link #TOO_MANY_OVERDUE_TASKS}</li>
 *   <li>{@link #UNEVEN_CONTRIBUTION}</li>
 *   <li>{@link #TOPIC_NOT_ASSIGNED}</li>
 *   <li>{@link #STALE_SYNC}</li>
 *   <li>{@link #STABLE}</li>
 * </ol>
 */
public enum PrimaryReason {

    /** Nhóm hoàn toàn ổn định, không có vấn đề nào được phát hiện. */
    STABLE,

    /** Không có commit nào trong tuần được theo dõi. */
    NO_ACTIVITY_THIS_WEEK,

    /** Tỉ lệ thành viên hoạt động quá thấp. */
    TOO_FEW_ACTIVE_MEMBERS,

    /** Có quá nhiều task quá hạn (overdue). */
    TOO_MANY_OVERDUE_TASKS,

    /**
     * Phân bổ commit không đều: một thành viên đóng góp
     * quá lớn tỉ lệ tổng commit của nhóm.
     */
    UNEVEN_CONTRIBUTION,

    /** Nhóm chưa được gán topic/đề tài. */
    TOPIC_NOT_ASSIGNED,

    /**
     * Dữ liệu sync (GitHub/Jira) cũ, có thể không phản ánh
     * tình trạng thực tế.
     */
    STALE_SYNC
}
