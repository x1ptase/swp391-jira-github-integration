package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO một Subtask item, trả về khi user expand Story.
 * Subtask = Task có parentTask != null và jiraIssueType = 'SUBTASK'.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubtaskItemResponse {

    /** PK của Subtask Task trong DB nội bộ */
    private Integer taskId;

    /** taskId của Story cha */
    private Integer parentStoryId;

    /** Jira Subtask key, e.g. "SWP391-25" */
    private String subtaskKey;

    /** Title/summary của Subtask */
    private String summary;

    // --- Status ---
    /** Internal status id */
    private Integer statusId;

    /** Internal status code, e.g. "TODO", "IN_PROGRESS", "DONE" */
    private String statusCode;

    /** Raw Jira status name, e.g. "Done" */
    private String statusRaw;

    // --- Assignee ---
    /** Internal userId của assignee. Nullable. */
    private Long assigneeId;

    /** Full name của assignee. Nullable. */
    private String assigneeName;

    /** Thời điểm update cuối từ Jira. Nullable. */
    private LocalDateTime updated;
}
