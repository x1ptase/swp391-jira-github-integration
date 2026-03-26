package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubCommitDetailedDTO {
    private String sha;
    private Stats stats;
    private List<Object> files;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Stats {
        private Integer additions;
        private Integer deletions;
        private Integer total;
    }
}
