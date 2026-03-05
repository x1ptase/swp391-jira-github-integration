package com.swp391.backend.service;

import com.swp391.backend.dto.request.CommitSearchRequest;
import com.swp391.backend.dto.response.GitHubCommitResponse;
import com.swp391.backend.dto.response.JiraProjectResponse;
import com.swp391.backend.entity.IntegrationConfig;

public interface IntegrationService {

    /** Save or update GitHub integration config for a group. */
    IntegrationConfig saveOrUpdate(Long groupId, String repoFullName, String token);

    /**
     * Save or update Jira integration config for a group.
     * On CREATE: token must be non-blank (400 if missing).
     * On UPDATE: token is optional; if null/blank, existing token is retained.
     */
    IntegrationConfig saveOrUpdateJira(Long groupId, String baseUrl, String projectKey,
            String jiraEmail, String token);

    /**
     * Tests the GitHub connection for a specific group.
     * 
     * @param groupId the ID of the group
     * @return repository information (full name and default branch)
     */
    com.swp391.backend.dto.response.GitHubRepoResponse testGitHubConnection(Long groupId);

    /**
     * Fetches commits from GitHub API based on search criteria (date range or last
     * N days).
     * Supports full pagination.
     */
    java.util.List<GitHubCommitResponse> fetchCommitsWithCriteria(Long groupId, CommitSearchRequest criteria);

    /**
     * Tests the Jira connection for a specific group using the saved Jira config.
     *
     * @param groupId the ID of the group
     * @return Jira project information (key and name)
     */
    JiraProjectResponse testJiraConnection(Long groupId);
}
