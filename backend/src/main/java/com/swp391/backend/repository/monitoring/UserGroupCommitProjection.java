package com.swp391.backend.repository.monitoring;

import java.time.LocalDateTime;

/**
 * Projection cho thống kê commit cá nhân theo nhóm trong khoảng thời gian.
 * Dùng bởi {@link MonitoringAggregationRepository}.
 */
public interface UserGroupCommitProjection {

    /** ID nhóm. */
    Long getGroupId();

    /** ID người dùng nội bộ (author_user_id đã được map). */
    Long getUserId();

    /** Số commit của người dùng trong nhóm và khoảng thời gian. */
    long getCommitCount();

    /** Thời điểm commit gần nhất của người dùng trong nhóm. */
    LocalDateTime getLastCommitAt();
}
