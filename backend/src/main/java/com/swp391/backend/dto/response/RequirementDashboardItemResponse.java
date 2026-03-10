package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO trả về cho dashboard requirement list theo group.
 * Chỉ chứa Requirement có jira_issue_type = 'EPIC'.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequirementDashboardItemResponse {

    /** PK của Requirement trong DB nội bộ */
    private Integer requirementId;

    /** Group sở hữu requirement này */
    private Long groupId;

    /** Jira Epic key, e.g. "SWP391-44" */
    private String epicKey;

    /** Title/summary của Epic */
    private String summary;

    // --- Status ---
    /** Internal status id */
    private Integer statusId;

    /** Internal status code, e.g. "ACTIVE" */
    private String statusCode;

    /** Raw Jira status name, e.g. "In Progress" */
    private String statusRaw;

    // --- Priority ---
    /** Internal priority id */
    private Integer priorityId;

    /** Internal priority code, e.g. "MEDIUM" */
    private String priorityCode;

    /** Raw Jira priority name, e.g. "Medium" */
    private String priorityRaw;

    /** Thời điểm update cuối từ Jira. Nullable. */
    private LocalDateTime updated;

    // --- Story progress ---
    /** Tổng số Story thuộc Epic này */
    private long storiesCount;

    /** Số Story có status DONE */
    private long doneStoriesCount;

    /** Alias của doneStoriesCount, dùng cho FE dashboard */
    private long progressDone;

    /** Alias của storiesCount, dùng cho FE dashboard */
    private long progressTotal;
}
