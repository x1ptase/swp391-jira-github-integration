package com.swp391.backend.service.impl;

import com.swp391.backend.common.IntegrationTypeIds;
import com.swp391.backend.dto.response.GitHubCommitDTO;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.entity.SyncLog;
import com.swp391.backend.exception.GitHubApiException;
import com.swp391.backend.integration.github.GitHubClient;
import com.swp391.backend.repository.IntegrationConfigRepository;
import com.swp391.backend.service.GitHubSyncService;
import com.swp391.backend.service.SyncLogService;
import com.swp391.backend.service.TokenCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubSyncServiceImpl implements GitHubSyncService {

    private final GitHubClient gitHubClient;
    private final SyncLogService syncLogService;
    private final IntegrationConfigRepository configRepository;
    private final TokenCryptoService tokenCryptoService;

    @Override
    public void syncCommits(Long groupId) {
        SyncLog logEntry = syncLogService.begin(groupId, "GITHUB");
        Long logId = logEntry.getId();

        try {
            // 1. Get Config
            IntegrationConfig config = configRepository
                    .findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.GITHUB)
                    .orElseThrow(() -> new RuntimeException("GitHub config not found"));

            // 2. Decrypt Token
            String token = tokenCryptoService.decryptFromBytes(config.getTokenEncrypted());

            // 3. Fetch Commits
            List<GitHubCommitDTO> commits = gitHubClient.fetchAllCommits(config.getRepoFullName(), token);

            // 4. Success Log
            syncLogService.success(logId, "Successfully fetched " + commits.size() + " commits", commits.size(), 0);

        } catch (GitHubApiException e) {
            syncLogService.fail(logId, e.getMessage());
            throw e;
        } catch (Exception e) {
            syncLogService.fail(logId, "Sync failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
