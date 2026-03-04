package com.swp391.backend.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Minimal issue ref returned by /rest/api/3/search/jql.
 * Only the id is needed to drive the subsequent bulkfetch call.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueIdRef {
    private String id;
}
