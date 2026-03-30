package com.swp391.backend.repository.monitoring;

/**
 * Projection cho thống kê số thành viên hoạt động theo nhóm trong khoảng thời gian.
 * "Active" = đã có ít nhất 1 commit trong khoảng thời gian.
 * Dùng bởi {@link MonitoringAggregationRepository}.
 */
public interface GroupActiveMemberCountProjection {

    /** ID nhóm. */
    Long getGroupId();

    /** Số thành viên đã có commit trong khoảng thời gian. */
    long getActiveMembers();
}
