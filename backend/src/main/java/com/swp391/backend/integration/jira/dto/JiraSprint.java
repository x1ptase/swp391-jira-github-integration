package com.swp391.backend.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents a Jira Sprint (from /rest/agile/1.0/board/{boardId}/sprint).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraSprint {

    private Long id;
    private String name;
    private String state;
    private String startDate;
    private String endDate;
    private String completeDate;
}
