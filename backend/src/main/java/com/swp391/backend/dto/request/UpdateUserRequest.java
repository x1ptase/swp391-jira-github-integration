package com.swp391.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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


    @Size(max = 255)
    private String jiraAccountId;

    public UpdateUserRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public String getJiraAccountId() { return jiraAccountId; }

    public void setJiraAccountId(String jiraAccountId) { this.jiraAccountId = jiraAccountId; }
}
