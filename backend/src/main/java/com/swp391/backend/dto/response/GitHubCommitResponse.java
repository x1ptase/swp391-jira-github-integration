package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubCommitResponse {
    private String sha;
    private String authorName;
    private String authorEmail;
    private String date;
    private String message;
}
