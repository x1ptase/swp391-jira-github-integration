package com.swp391.backend.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Represents Jira issue fields from the Search API v3.
 * description is Object because Jira returns ADF (Atlassian Document Format)
 * JSON.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraFields {

    private String summary;

    /** ADF object – can be a Map<String, Object> or null. */
    private Object description;

    private JiraName issuetype;
    private JiraName status;
    private JiraName priority; // nullable
    private JiraAssignee assignee; // nullable
    private String updated;

    /** Labels attached to the issue. */
    private List<String> labels;
}
