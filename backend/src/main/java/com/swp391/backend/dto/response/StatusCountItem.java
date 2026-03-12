package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Một dòng trong byStatus breakdown của StoryProgressSummaryResponse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusCountItem {

    /** Internal status id */
    private Integer statusId;

    /** Internal status code, e.g. "TODO", "IN_PROGRESS", "DONE" */
    private String statusCode;

    /** Số Story có status này */
    private long count;
}
