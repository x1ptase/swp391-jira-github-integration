package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal response DTO for a Jira Board (id + name only).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraBoardDto {

    private Long id;
    private String name;
}
