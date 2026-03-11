package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Thống kê commit của một tác giả trong một nhóm.
 *
 * <p>
 * <b>Logic nhóm (Grouping priority):</b><br>
 * Nếu commit đã được map với User nội bộ ({@code authorUserId != null}),
 * nhóm theo {@code authorUserId}.<br>
 * Nếu chưa map được, nhóm theo {@code authorEmail} (fallback
 * {@code authorName}).
 * </p>
 *
 * <p>
 * Các trường {@code additions}, {@code deletions}, {@code filesChanged}
 * có thể null nếu chưa có dữ liệu detail (GitHub API list endpoint
 * không trả về stats, chỉ commit detail endpoint mới có).
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommitStatsDTO {

    // ── Định danh tác giả ───────────────────────────────────────────────────

    /** ID User nội bộ; null nếu chưa map được với tài khoản trong hệ thống. */
    private Long authorUserId;

    /** Họ tên đầy đủ của User nội bộ (lấy từ bảng Users). Null nếu chưa map. */
    private String authorFullName;

    /** Username nội bộ. Null nếu chưa map. */
    private String authorUsername;

    /** Tên tác giả từ Git commit (git config user.name). */
    private String authorName;

    /** Email tác giả từ Git commit. */
    private String authorEmail;

    /** GitHub login (username trên GitHub). */
    private String authorLogin;

    // ── Số liệu thống kê ────────────────────────────────────────────────────

    /** Tổng số commit của tác giả này trong nhóm. */
    private Long commitCount;

    /** Tổng số dòng thêm (additions). Null nếu chưa có dữ liệu. */
    private Long totalAdditions;

    /** Tổng số dòng xóa (deletions). Null nếu chưa có dữ liệu. */
    private Long totalDeletions;

    /** Tổng số file thay đổi (files_changed). Null nếu chưa có dữ liệu. */
    private Long totalFilesChanged;

    /** Ngày/giờ commit gần nhất của tác giả này. */
    private LocalDateTime latestCommitDate;

    // ── Factory method ──────────────────────────────────────────────────────

    /**
     * Chuyển đổi từ interface projection sang DTO class.
     * Dùng sau khi lấy kết quả từ
     * {@code GitCommitRepository.getCommitStatsByGroup()}.
     */
    public static CommitStatsDTO from(CommitStatsProjection p) {
        return CommitStatsDTO.builder()
                .authorUserId(p.getAuthorUserId())
                .authorFullName(p.getAuthorFullName())
                .authorUsername(p.getAuthorUsername())
                .authorName(p.getAuthorName())
                .authorEmail(p.getAuthorEmail())
                .authorLogin(p.getAuthorLogin())
                .commitCount(p.getCommitCount())
                .totalAdditions(p.getTotalAdditions())
                .totalDeletions(p.getTotalDeletions())
                .totalFilesChanged(p.getTotalFilesChanged())
                .latestCommitDate(p.getLatestCommitDate())
                .build();
    }
}
