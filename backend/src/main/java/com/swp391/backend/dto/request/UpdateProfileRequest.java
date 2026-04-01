package com.swp391.backend.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateProfileRequest {

    @Email(message = "Email is invalid")
    @Size(max = 120, message = "Email max length is 120")
    private String email;

    @Size(max = 100, message = "Github username max length is 100")
    private String githubUsername;

    @Size(max = 255, message = "Jira AccountId max length is 100")
    private String jiraAccountId;
}
