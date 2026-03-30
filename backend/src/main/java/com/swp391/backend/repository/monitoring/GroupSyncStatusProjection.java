package com.swp391.backend.repository.monitoring;

import java.time.LocalDateTime;

/**
 * Projection cho thông tin sync gần nhất của một nhóm theo source.
 * Dùng bởi {@link MonitoringAggregationRepository}.
 */
public interface GroupSyncStatusProjection {

    /** ID nhóm. */
    Long getGroupId();

    /** Source của sync: "GITHUB" hoặc "JIRA". */
    String getSource();

    /** Thời điểm sync thành công gần nhất; null nếu chưa sync thành công. */
    LocalDateTime getLastSyncAt();
}
