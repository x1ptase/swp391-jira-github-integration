package com.swp391.backend.dto.response;

import java.time.LocalDateTime;

/**
 * Interface-based projection để Spring Data JPA tự map kết quả native query.
 *
 * <p>
 * Tên các getter method PHẢI khớp chính xác (case-insensitive) với alias
 * trong câu SQL của {@code GitCommitRepository.getCommitStatsByGroup()}.
 * </p>
 *
 * <p>
 * Để sử dụng ở tầng Service/Controller, convert sang {@link CommitStatsDTO}
 * bằng {@code CommitStatsDTO.from(projection)}.
 * </p>
 */
public interface CommitStatsProjection {

    Long getAuthorUserId();

    String getAuthorFullName();

    String getAuthorUsername();

    String getAuthorName();

    String getAuthorEmail();

    String getAuthorLogin();

    Long getCommitCount();

    Long getTotalAdditions();

    Long getTotalDeletions();

    Long getTotalFilesChanged();

    LocalDateTime getLatestCommitDate();
}
