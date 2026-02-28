package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JiraIntegrationResponse {

    private String baseUrl;
    private String projectKey;
    private String jiraEmail;

    /** True if token_encrypted is stored in DB; false otherwise. */
    private boolean hasToken;

    /**
     * Masked representation of the stored token (e.g. "****abcd").
     * Null when hasToken is false.
     * NEVER contains the raw token value.
     */
    private String tokenMasked;
}
