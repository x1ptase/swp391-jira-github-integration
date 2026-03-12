package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO detail của một Subtask trong màn "My Work".
 * Trả về khi user click vào một Subtask để xem chi tiết.
 * Bao gồm context Story cha + Epic cha.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyWorkSubtaskDetailResponse {

    /** PK của Subtask Task trong DB nội bộ */
    private Integer taskId;

    /** Jira Subtask key, e.g. "SWP391-25". Nullable. */
    private String subtaskKey;

    /** Title/summary của Subtask */
    private String summary;

    /** Description đầy đủ. Nullable. */
    private String description;

    // --- Status ---
    /** Internal status id */
    private Integer statusId;

    /** Internal status code, e.g. "TODO", "IN_PROGRESS", "DONE" */
    private String statusCode;

    /** Raw Jira status name, e.g. "In Progress". Nullable. */
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

    // --- Parent Story context ---
    /** taskId của Story cha. Nullable. */
    private Integer parentStoryId;

    /** Jira key của Story cha. Nullable. */
    private String parentStoryKey;

    /** Title/summary của Story cha. Nullable. */
    private String parentStorySummary;

    // --- Parent Epic context ---
    /** requirementId của Epic cha. Nullable. */
    private Integer requirementId;

    /** Jira key của Epic cha. Nullable. */
    private String epicKey;

    /** Title/summary của Epic cha. Nullable. */
    private String epicSummary;
}
