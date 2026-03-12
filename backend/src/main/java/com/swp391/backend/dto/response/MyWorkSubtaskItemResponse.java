package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO một Subtask item trong danh sách "My Work" của Team Member.
 * Chứa đủ: subtask info + parent Story context + parent Epic context.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyWorkSubtaskItemResponse {

    /** PK của Subtask Task trong DB nội bộ */
    private Integer taskId;

    /** taskId của Story cha. Nullable nếu parentTask bị null (dữ liệu lỗi). */
    private Integer parentStoryId;

    /** requirementId (Epic) của Story cha. Nullable. */
    private Integer requirementId;

    // --- Subtask info ---

    /** Jira Subtask key, e.g. "SWP391-25". Nullable. */
    private String subtaskKey;

    /** Title/summary của Subtask */
    private String summary;

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

    // --- Parent Story context ---
    /** Jira key của Story cha, e.g. "SWP391-10". Nullable. */
    private String parentStoryKey;

    /** Title/summary của Story cha. Nullable. */
    private String parentStorySummary;

    // --- Parent Epic context ---
    /** Jira key của Epic cha (Requirement), e.g. "SWP391-1". Nullable. */
    private String epicKey;

    /** Title/summary của Epic cha (Requirement). Nullable. */
    private String epicSummary;

    /** Thời điểm update cuối từ Jira. Nullable. */
    private LocalDateTime updated;
}
