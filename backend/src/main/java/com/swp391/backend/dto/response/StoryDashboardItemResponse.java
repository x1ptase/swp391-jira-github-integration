package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO một Story item trong dashboard list.
 * Chỉ Story top-level (parentTask IS NULL, jiraIssueType = 'STORY').
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryDashboardItemResponse {

    /** PK của Task trong DB nội bộ */
    private Integer taskId;

    /** Requirement (Epic) parent */
    private Integer requirementId;

    /** Group sở hữu story này */
    private Long groupId;

    /** Jira Story key, e.g. "SWP391-10" */
    private String storyKey;

    /** Title/summary của Story */
    private String summary;

    // --- Status ---
    /** Internal status id */
    private Integer statusId;

    /** Internal status code, e.g. "TODO", "IN_PROGRESS", "DONE" */
    private String statusCode;

    /** Raw Jira status name, e.g. "In Progress" */
    private String statusRaw;

    // --- Priority ---
    /** Raw Jira priority name, e.g. "High". Nullable. */
    private String priorityRaw;

    // --- Assignee ---
    /** Internal userId của assignee. Nullable. */
    private Long assigneeId;

    /** Full name của assignee. Nullable. */
    private String assigneeName;

    /** Thời điểm update cuối từ Jira. Nullable. */
    private LocalDateTime updated;

    /** Tổng số Subtask con của Story này. */
    private long subtasksCount;
}
