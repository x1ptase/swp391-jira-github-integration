package com.swp391.backend.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for /rest/api/3/search/jql (Jira Cloud new paginated search).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraSearchJqlResponse {

    /** List of issue refs (only id field). */
    private List<JiraIssueIdRef> issues;

    /** Opaque token to pass as nextPageToken on the next call. Null when no more pages. */
    private String nextPageToken;

    /** True if this is the last page. */
    private Boolean isLast;
}
