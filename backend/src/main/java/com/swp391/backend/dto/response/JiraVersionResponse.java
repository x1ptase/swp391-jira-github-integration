package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a Jira Version (returned to API consumers).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraVersionResponse {

    private String id;
    private String name;
    private Boolean released;
    private Boolean archived;
    private String releaseDate;
    private String description;
}
