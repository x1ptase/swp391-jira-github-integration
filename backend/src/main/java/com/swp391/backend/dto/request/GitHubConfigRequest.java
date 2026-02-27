package com.swp391.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubConfigRequest {
    @NotBlank(message = "repoFullName is required")
    private String repoFullName;

    private String token;
}
