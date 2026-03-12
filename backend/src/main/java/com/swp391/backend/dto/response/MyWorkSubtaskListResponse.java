package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * Top-level response cho endpoint "My Work – Subtasks".
 *
 * <p>Thiết kế để FE biết rõ 3 trạng thái:
 * <ul>
 *   <li>mappedToJira=false → user chưa map Jira account (unmapped state)</li>
 *   <li>mappedToJira=true, page rỗng → user đã map nhưng không có subtask nào</li>
 *   <li>mappedToJira=true, page có data → bình thường</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyWorkSubtaskListResponse {

    /**
     * true nếu current user đã map với Jira account (jiraAccountId != null/blank).
     * false nếu chưa map → FE hiển thị unmapped state.
     */
    private boolean mappedToJira;

    /** Internal userId của current user */
    private Long currentUserId;

    /**
     * jiraAccountId của current user.
     * null nếu user chưa map Jira.
     */
    private String currentUserJiraAccountId;

    /** Page of subtask items. Rỗng nếu user chưa map hoặc không có subtask. */
    private Page<MyWorkSubtaskItemResponse> page;
}
