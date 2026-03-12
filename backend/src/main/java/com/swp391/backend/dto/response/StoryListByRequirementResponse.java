package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * Top-level response cho endpoint GET .../requirements/{requirementId}/stories.
 * Bọc epic metadata + progress summary + paginated story list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryListByRequirementResponse {

    /** PK của Requirement (Epic) trong DB nội bộ */
    private Integer requirementId;

    /** Jira Epic key, e.g. "SWP391-44" */
    private String epicKey;

    /** Title/summary của Epic */
    private String epicSummary;

    /** Progress summary tính theo filter đang áp dụng */
    private StoryProgressSummaryResponse progressSummary;

    /** Paginated danh sách Story item */
    private Page<StoryDashboardItemResponse> page;
}
