package com.swp391.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubCommitDTO {

    private String sha;

    private CommitInfo commit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommitInfo {
        private String message;
        private Author author;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Author {
        private String name;
        private String email;
        private String date;
    }
}
