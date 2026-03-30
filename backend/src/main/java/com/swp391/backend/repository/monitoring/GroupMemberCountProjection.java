package com.swp391.backend.repository.monitoring;

/**
 * Projection cho thống kê số thành viên theo nhóm.
 * Dùng bởi {@link MonitoringAggregationRepository}.
 */
public interface GroupMemberCountProjection {

    /** ID nhóm. */
    Long getGroupId();

    /** Tổng số thành viên. */
    long getTotalMembers();
}
