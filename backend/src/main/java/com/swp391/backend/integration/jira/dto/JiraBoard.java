package com.swp391.backend.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Represents a Jira Agile board (from /rest/agile/1.0/board).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraBoard {

    private Long id;
    private String name;
    private String type;
}
