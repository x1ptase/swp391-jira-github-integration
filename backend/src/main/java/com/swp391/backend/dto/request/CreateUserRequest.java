package com.swp391.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username max length is 50")
    private String username;

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


    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password length must be 6-72")
    private String password;


    @Size(max = 100, message = "Github username max length is 100")
    private String githubUsername;


    @Size(max = 255, message = "Jira account id max length is 255")
    private String jiraAccountId;

    public CreateUserRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
