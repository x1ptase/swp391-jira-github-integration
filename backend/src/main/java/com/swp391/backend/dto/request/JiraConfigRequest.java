package com.swp391.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraConfigRequest {

    @NotBlank(message = "baseUrl is required")
    private String baseUrl;

    @NotBlank(message = "projectKey is required")
    private String projectKey;

    @NotBlank(message = "jiraEmail is required")
    @Email(message = "jiraEmail must be a valid email address")
    private String jiraEmail;

    /**
     * Token is required on CREATE; optional on UPDATE.
     * If null/blank on UPDATE, existing token_encrypted is retained.
     * This field is NEVER returned in responses.
     */
    private String token;
}
