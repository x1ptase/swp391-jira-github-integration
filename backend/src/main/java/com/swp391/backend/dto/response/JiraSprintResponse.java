package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a Jira Sprint (returned to API consumers).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraSprintResponse {
    private Long id;
    private String name;
    private String state;
    private String startDate;
    private String endDate;
    private String completeDate;
}
