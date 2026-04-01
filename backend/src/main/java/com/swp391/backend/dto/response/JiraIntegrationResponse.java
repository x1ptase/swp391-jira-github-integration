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

    private boolean hasToken;

    private String tokenMasked;
}
