package com.swp391.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response wrapper for single-page Jira issue fetch (fetchAll=false).
 * Contains items + pagination info for FE to call next page.
 */
@Data
@Builder
public class JiraIssuePageResponse {

    private List<JiraIssueExportDto> items;

    /** Opaque token to pass as pageToken on the next request. Null if no more pages. */
    private String nextPageToken;

    /** True if this is the last page. */
    private Boolean isLast;
}
