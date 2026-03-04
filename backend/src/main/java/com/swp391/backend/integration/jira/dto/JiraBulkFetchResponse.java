package com.swp391.backend.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for POST /rest/api/3/issue/bulkfetch.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraBulkFetchResponse {
    private List<JiraIssue> issues;
}
