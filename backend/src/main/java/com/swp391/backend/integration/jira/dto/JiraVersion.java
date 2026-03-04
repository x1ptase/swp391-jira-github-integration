package com.swp391.backend.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents a Jira project version (from
 * /rest/api/3/project/{projectKey}/versions).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraVersion {

    private String id;
    private String name;
    private Boolean released;
    private Boolean archived;
    private String releaseDate;
    private String description;
}
