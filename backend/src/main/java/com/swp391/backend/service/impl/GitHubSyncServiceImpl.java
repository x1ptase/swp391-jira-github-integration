package com.swp391.backend.service.impl;

import com.swp391.backend.utils.IntegrationTypeIds;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai đồng bộ commit từ GitHub về database.
 *
 * <h3>Luồng xử lý sync</h3>
 * 
 * <pre>
 *  begin(groupId, "GITHUB")
 *      ├─ [409 CONFLICT] → throw ngay lên Controller (không có syncId → không cần end())
 *      └─ [OK] → syncId được lưu
 *           try { doSync() }          → end(SUCCESS)
 *           catch (ResponseStatusEx)  { end(FAILED) → rethrow }
 *           catch (Exception)         { end(FAILED) → wrap 500 → rethrow }
 * </pre>
 *
 * <p>
 * <b>Không có @Transactional ở method level</b> vì mỗi lệnh gọi
 * {@link SyncLogService} đã dùng REQUIRES_NEW transaction riêng.
 * Nếu bọc thêm transaction cha, end(FAILED) trong catch không commit được
 * khi transaction đang trong trạng thái rollback-only.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubSyncServiceImpl implements GitHubSyncService {

    private final SyncLogService syncLogService;
    private final IntegrationConfigRepository configRepository;
    private final TokenCryptoService tokenCryptoService;
    private final GitHubClient gitHubClient;
    private final RepositoryRepository repositoryRepository;
    private final GitCommitRepository gitCommitRepository;
    private final UserRepository userRepository;

    // -------------------------------------------------------------------------
    // syncNow
    // -------------------------------------------------------------------------

    @Override
    public SyncResultResponse syncNow(Long groupId) {
        // ── Bước 1: Bắt đầu SyncLog ─────────────────────────────────────────
        // Nếu begin() ném 409 CONFLICT (đang có RUNNING), exception đi thẳng lên
        // Controller mà KHÔNG qua các catch bên dưới → không cần gọi end().
        SyncLog syncLog = syncLogService.begin(groupId, "GITHUB");
        Long syncId = syncLog.getId();

        int insertedCount = 0;
        int updatedCount = 0;

        try {
            // ── Bước 2: Lấy cấu hình & giải mã token ─────────────────────
            IntegrationConfig config = configRepository
                    .findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.GITHUB)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "GitHub integration not configured"));

            if (config.getRepoFullName() == null || config.getTokenEncrypted() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Incomplete GitHub configuration");
            }

            String token = tokenCryptoService.decryptFromBytes(config.getTokenEncrypted());

            // ── Bước 3: Lấy/tạo Repository record ───────────────────────
            Repository repo = repositoryRepository
                    .findByGroupIdAndFullName(groupId, config.getRepoFullName())
                    .orElseGet(() -> repositoryRepository.save(
                            Repository.builder()
                                    .groupId(groupId)
                                    .fullName(config.getRepoFullName())
                                    .build()));

            // ── Bước 4: Gọi GitHub API (có phân trang bên trong) ─────────
            List<GitHubCommitDTO> commits = gitHubClient.fetchAllCommits(config.getRepoFullName(), token);

            // ── Bước 5: Upsert commits ───────────────────────────────────
            for (GitHubCommitDTO dto : commits) {
                // Bỏ qua commit không có SHA (dữ liệu dị thường từ API)
                if (dto.getSha() == null) {
                    log.warn("[GitHub Sync] Skipping commit with null SHA, group={}", groupId);
                    continue;
                }

                // Bỏ qua commit không có date (null sẽ vi phạm NOT NULL constraint)
                if (dto.getCommit() == null
                        || dto.getCommit().getAuthor() == null
                        || dto.getCommit().getAuthor().getDate() == null) {
                    log.warn("[GitHub Sync] Skipping commit {} – missing author/date info", dto.getSha());
                    continue;
                }

                Optional<GitCommit> existingOpt = gitCommitRepository.findByRepoIdAndSha(repo.getRepoId(),
                        dto.getSha());

                GitCommit commit = existingOpt.orElseGet(() -> GitCommit.builder()
                        .repoId(repo.getRepoId())
                        .sha(dto.getSha())
                        .build());

                commit.setAuthorName(dto.getCommit().getAuthor().getName());
                commit.setAuthorEmail(dto.getCommit().getAuthor().getEmail());
                commit.setCommitDate(LocalDateTime.parse(
                        dto.getCommit().getAuthor().getDate(),
                        DateTimeFormatter.ISO_DATE_TIME));
                commit.setMessage(dto.getCommit().getMessage());

                if (dto.getAuthor() != null) {
                    commit.setAuthorLogin(dto.getAuthor().getLogin());
                }

                // Liên kết với User nội bộ nếu email khớp
                if (commit.getAuthorEmail() != null) {
                    userRepository.findByEmailIgnoreCase(commit.getAuthorEmail())
                            .ifPresent(u -> commit.setAuthorUserId(u.getUserId()));
                }

                gitCommitRepository.save(commit);

                if (existingOpt.isPresent())
                    updatedCount++;
                else
                    insertedCount++;
            }

            // ── Bước 6: Kết thúc thành công ─────────────────────────────
            String successMsg = "Synced " + (insertedCount + updatedCount) + " commit(s) successfully";
            syncLogService.end(syncId, SyncStatus.SUCCESS, insertedCount, updatedCount, successMsg);
            log.info("[GitHub Sync] group={} SUCCESS – inserted={}, updated={}", groupId, insertedCount, updatedCount);

            return SyncResultResponse.builder()
                    .status("SUCCESS")
                    .insertedCount(insertedCount)
                    .updatedCount(updatedCount)
                    .message(successMsg)
                    .build();

        } catch (ResponseStatusException ex) {
            // Lỗi có HTTP status rõ ràng (404 config, 400 bad config, 401 token,
            // 429 rate-limit từ GitHubClient, 409 concurrency guard…)
            String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
            log.error("[GitHub Sync] group={} FAILED [{}]: {}", groupId, ex.getStatusCode(), reason);
            syncLogService.end(syncId, SyncStatus.FAILED, insertedCount, updatedCount, reason);
            throw ex; // giữ nguyên HTTP status, trả lên Controller

        } catch (Exception ex) {
            // Lỗi không mong đợi: network timeout, decrypt fail, DB error…
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            log.error("[GitHub Sync] group={} unexpected error: {}", groupId, msg, ex);
            syncLogService.end(syncId, SyncStatus.FAILED, insertedCount, updatedCount, msg);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Sync failed: " + msg, ex);
        }
    }
}
