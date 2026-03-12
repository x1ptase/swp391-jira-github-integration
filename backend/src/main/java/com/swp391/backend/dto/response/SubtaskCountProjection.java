package com.swp391.backend.dto.response;

/**
 * Projection batch-count subtask theo parentTaskId.
 * Tránh N+1 khi build StoryDashboardItemResponse.
 */
public interface SubtaskCountProjection {
    Integer getParentTaskId();

    long getCount();
}
