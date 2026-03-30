package com.swp391.backend.repository.monitoring;

import java.time.LocalDateTime;

/**
 * Projection cho thống kê commit theo nhóm trong khoảng thời gian.
 * Dùng bởi {@link MonitoringAggregationRepository}.
 */
public interface GroupCommitSummaryProjection {

    /** ID nhóm. */
    Long getGroupId();

    /** Tổng số commit trong khoảng thời gian. */
    long getTotalCommits();

    /** Thời điểm commit gần nhất; null nếu chưa có commit. */
    LocalDateTime getLastCommitAt();
}
