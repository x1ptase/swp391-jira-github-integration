package com.swp391.backend.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for /rest/agile/1.0/board/{boardId}/sprint.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraSprintListResponse {

    private List<JiraSprint> values;
    private Integer startAt;
    private Integer maxResults;
    private Integer total;
    private Boolean isLast;
}
