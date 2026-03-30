package com.swp391.backend.repository.monitoring;

/**
 * Projection cho thống kê số task overdue theo nhóm.
 * "Overdue" = due_date &lt; NOW() VÀ status không phải DONE.
 * Dùng bởi {@link MonitoringAggregationRepository}.
 */
public interface GroupOverdueTaskCountProjection {

    /** ID nhóm. */
    Long getGroupId();

    /** Số task quá hạn. */
    long getOverdueTasks();
}
