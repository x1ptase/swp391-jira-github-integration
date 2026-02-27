package com.swp391.backend.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationResponse {
    private String repoFullName;
    private boolean hasToken;
    private String tokenMasked;
}
