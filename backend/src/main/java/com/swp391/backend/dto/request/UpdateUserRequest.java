package com.swp391.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    @Size(max = 120, message = "Email max length is 120")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name max length is 100")
    private String fullName;

    @NotBlank(message = "Role code is required")
    @Size(max = 30, message = "Role code max length is 30")
    private String roleCode;


    @Size(max = 100, message = "Github username max length is 100")
    private String githubUsername;


    @Email(message = "Jira email is invalid")
    @Size(max = 120, message = "Jira email max length is 120")
    private String jiraEmail;

    public UpdateUserRequest() {
    }

}
