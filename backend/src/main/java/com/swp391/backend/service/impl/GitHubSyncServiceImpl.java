package com.swp391.backend.service.impl;

import com.swp391.backend.common.IntegrationTypeIds;
import com.swp391.backend.dto.response.GitHubCommitDTO;
import com.swp391.backend.dto.response.SyncResultResponse;
import com.swp391.backend.entity.*;
import com.swp391.backend.integration.github.GitHubClient;
import com.swp391.backend.repository.GitCommitRepository;
import com.swp391.backend.repository.IntegrationConfigRepository;
import com.swp391.backend.repository.RepositoryRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.service.GitHubSyncService;
import com.swp391.backend.service.SyncLogService;
import com.swp391.backend.service.TokenCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubSyncServiceImpl implements GitHubSyncService {

    private final SyncLogService syncLogService;
    private final IntegrationConfigRepository configRepository;
    private final TokenCryptoService tokenCryptoService;
    private final GitHubClient gitHubClient;
    private final RepositoryRepository repositoryRepository;
    private final GitCommitRepository gitCommitRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public SyncResultResponse syncNow(Long groupId) {
        // Concurrency Guard
        SyncLog syncLog = syncLogService.begin(groupId, "GITHUB");
        int insertedCount = 0;
        int updatedCount = 0;
        String errorMessage = null;

        try {
            // Token & Repo
            IntegrationConfig config = configRepository
                    .findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.GITHUB)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "GitHub integration not configured"));

            if (config.getRepoFullName() == null || config.getTokenEncrypted() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incomplete GitHub configuration");
            }

            String token = tokenCryptoService.decryptFromBytes(config.getTokenEncrypted());

            Repository repo = repositoryRepository.findByGroupIdAndFullName(groupId, config.getRepoFullName())
                    .orElseGet(() -> {
                        Repository newRepo = Repository.builder()
                                .groupId(groupId)
                                .fullName(config.getRepoFullName())
                                .build();
                        return repositoryRepository.save(newRepo);
                    });

            // Fetch list of commits (Pagination is handled entirely inside
            // GitHubClient.fetchAllCommits)
            List<GitHubCommitDTO> commits = gitHubClient.fetchAllCommits(config.getRepoFullName(), token);

            for (GitHubCommitDTO commitDto : commits) {
                Optional<GitCommit> existingOpt = gitCommitRepository.findByRepoIdAndSha(repo.getRepoId(),
                        commitDto.getSha());

                GitCommit gitCommit = existingOpt.orElseGet(() -> GitCommit.builder()
                        .repoId(repo.getRepoId())
                        .sha(commitDto.getSha())
                        .build());

                // Set author information
                if (commitDto.getCommit() != null && commitDto.getCommit().getAuthor() != null) {
                    gitCommit.setAuthorName(commitDto.getCommit().getAuthor().getName());
                    gitCommit.setAuthorEmail(commitDto.getCommit().getAuthor().getEmail());

                    if (commitDto.getCommit().getAuthor().getDate() != null) {
                        gitCommit.setCommitDate(LocalDateTime.parse(commitDto.getCommit().getAuthor().getDate(),
                                DateTimeFormatter.ISO_DATE_TIME));
                    }
                }

                if (commitDto.getAuthor() != null) {
                    gitCommit.setAuthorLogin(commitDto.getAuthor().getLogin());
                }

                if (commitDto.getCommit() != null) {
                    gitCommit.setMessage(commitDto.getCommit().getMessage());
                }

                // If email matches User, set author_user_id
                if (gitCommit.getAuthorEmail() != null) {
                    Optional<User> userOpt = userRepository.findByEmailIgnoreCase(gitCommit.getAuthorEmail());
                    userOpt.ifPresent(user -> gitCommit.setAuthorUserId(user.getUserId()));
                }

                gitCommitRepository.save(gitCommit);

                // Counters
                if (existingOpt.isPresent()) {
                    updatedCount++;
                } else {
                    insertedCount++;
                }
            }

        } catch (Exception e) {
            log.error("GitHub sync failed for group {}: {}", groupId, e.getMessage());
            errorMessage = e.getMessage();
            if (e instanceof ResponseStatusException) {
                throw e;
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Sync failed: " + e.getMessage());
        } finally {
            if (errorMessage == null) {
                syncLogService.updateStatus(syncLog.getId(), SyncStatus.SUCCESS, "Synced successfully", insertedCount,
                        updatedCount);
            } else {
                syncLogService.updateStatus(syncLog.getId(), SyncStatus.FAILED, errorMessage, insertedCount,
                        updatedCount);
            }
        }

        return SyncResultResponse.builder()
                .status(errorMessage == null ? "SUCCESS" : "FAILED")
                .insertedCount(insertedCount)
                .updatedCount(updatedCount)
                .message(errorMessage == null ? "Synced successfully" : errorMessage)
                .build();
    }
}
