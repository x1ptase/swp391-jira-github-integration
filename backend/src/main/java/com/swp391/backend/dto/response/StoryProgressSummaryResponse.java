package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Progress summary của Story list theo Epic (hoặc sau khi apply filter).
 * doneCount tính theo internal TaskStatus.code = 'DONE'.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryProgressSummaryResponse {

    /** Số Story có status DONE (theo internal TaskStatus.code) */
    private long doneCount;

    /** Tổng số Story (sau khi apply cùng filters) */
    private long totalCount;

    /** Breakdown count theo từng status */
    private List<StatusCountItem> byStatus;
}
