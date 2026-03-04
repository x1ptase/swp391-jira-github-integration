package com.swp391.backend.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for /rest/agile/1.0/board (board listing).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraBoardListResponse {

    private List<JiraBoard> values;
    private Integer startAt;
    private Integer maxResults;
    private Integer total;
    private Boolean isLast;
}
