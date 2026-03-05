package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal response DTO for a Jira Sprint (id + name only).
 * Distinct from {@link JiraSprintResponse} which carries full sprint metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraSprintDto {
    private Long id;
    private String name;
}
