package com.swp391.backend.service;

import com.swp391.backend.dto.request.CommitSearchRequest;
import com.swp391.backend.dto.response.GitHubCommitResponse;
import com.swp391.backend.dto.response.JiraProjectResponse;
import com.swp391.backend.entity.IntegrationConfig;

public interface IntegrationService {

    IntegrationConfig saveOrUpdate(Long groupId, String repoFullName, String token);

    IntegrationConfig saveOrUpdateJira(Long groupId, String baseUrl, String projectKey,
            String jiraEmail, String token);

    com.swp391.backend.dto.response.GitHubRepoResponse testGitHubConnection(Long groupId);

    java.util.List<GitHubCommitResponse> fetchCommitsWithCriteria(Long groupId, CommitSearchRequest criteria);

    JiraProjectResponse testJiraConnection(Long groupId);
}
