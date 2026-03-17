package com.swp391.backend.dto.request;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateProfileRequest {
    private String githubUsername;
    private String jiraAccountId;
}
