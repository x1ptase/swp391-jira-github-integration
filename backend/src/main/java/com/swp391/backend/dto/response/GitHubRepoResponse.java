package com.swp391.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubRepoResponse {

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("default_branch")
    private String defaultBranch;
}
