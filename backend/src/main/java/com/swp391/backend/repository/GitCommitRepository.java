package com.swp391.backend.repository;

import com.swp391.backend.dto.response.CommitStatsProjection;
import com.swp391.backend.dto.response.PersonalCommitStatsProjection;
import com.swp391.backend.entity.GitCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho bảng GitCommit.
 *
 * <h3>Chiến lược query thống kê</h3>
 * <p>
 * Query {@code getCommitStatsByGroup} dùng <b>native SQL (SQL Server)</b> vì
 * JPQL không
 * hỗ trợ GROUP BY trên expression COALESCE xuyên qua nhiều kiểu dữ liệu khác
 * nhau.
 * </p>
 *
 * <p>
 * <b>Grouping priority:</b><br>
 * 1. {@code author_user_id IS NOT NULL} → nhóm theo {@code author_user_id}<br>
 * 2. Fallback → nhóm theo {@code author_email} (nếu null thì
 * {@code author_name})<br>
 * Cột {@code group_key} dùng prefix ('uid:', 'email:', 'name:') để tránh va
 * chạm key.
 * </p>
 */
@Repository
public interface GitCommitRepository extends JpaRepository<GitCommit, Integer> {

    /** Tìm commit theo repo + SHA (dùng cho upsert trong GitHubSyncService). */
    Optional<GitCommit> findByRepoIdAndSha(Integer repoId, String sha);

    /**
     * Thống kê commit theo tác giả trong một nhóm sinh viên.
     *
     * <p>
     * Cách hoạt động:
     * <ol>
     * <li>JOIN {@code Repository} để lọc theo {@code group_id}</li>
     * <li>LEFT JOIN {@code Users} để lấy thông tin User nội bộ khi đã map</li>
     * <li>GROUP BY theo {@code group_key} được tính từ CASE WHEN:
     * <ul>
     * <li>Đã map User: key = {@code 'uid:<user_id>'}</li>
     * <li>Chưa map, có email: key = {@code 'email:<email>'}</li>
     * <li>Chưa map, không có email: key = {@code 'name:<author_name>'}</li>
     * </ul>
     * </li>
     * </ol>
     * </p>
     *
     * @param groupId ID nhóm sinh viên
     * @return danh sách thống kê, sắp xếp giảm dần theo số commit
     */
    @Query(value = """
            SELECT
                u.user_id                                       AS authorUserId,
                u.full_name                                     AS authorFullName,
                u.username                                      AS authorUsername,
                MAX(gc.author_name)                             AS authorName,
                MAX(gc.author_email)                            AS authorEmail,
                MAX(gc.author_login)                            AS authorLogin,
                COUNT(gc.commit_id)                             AS commitCount,
                SUM(ISNULL(gc.additions,     0))                AS totalAdditions,
                SUM(ISNULL(gc.deletions,     0))                AS totalDeletions,
                SUM(ISNULL(gc.files_changed, 0))                AS totalFilesChanged,
                MAX(gc.commit_date)                             AS latestCommitDate
            FROM GitCommit gc
            JOIN Repository r  ON r.repo_id = gc.repo_id
            LEFT JOIN Users  u  ON u.user_id = gc.author_user_id
            WHERE r.group_id = :groupId
            GROUP BY
                CASE
                    WHEN gc.author_user_id IS NOT NULL
                        THEN CAST('uid:'   + CAST(gc.author_user_id AS NVARCHAR(20))  AS NVARCHAR(200))
                    WHEN gc.author_email IS NOT NULL
                        THEN CAST('email:' + gc.author_email                           AS NVARCHAR(200))
                    ELSE
                        CAST('name:'  + ISNULL(gc.author_name, '(unknown)')           AS NVARCHAR(200))
                END,
                u.user_id,
                u.full_name,
                u.username
            ORDER BY commitCount DESC
            """, nativeQuery = true)
    List<CommitStatsProjection> getCommitStatsByGroup(@Param("groupId") Long groupId);

    /**
     * Lấy thống kê commit cá nhân dựa trên repository và người dùng.
     *
     * @param repoId ID của repository
     * @param userId ID của người dùng hệ thống (đã được map)
     * @return kết quả thống kê (duy nhất 1 dòng)
     */
    /**
     * Lấy thống kê commit cá nhân dựa trên groupId và người dùng.
     * Kết quả được tổng hợp từ tất cả repository của group đó.
     *
     * @param groupId ID của nhóm
     * @param userId ID của người dùng hệ thống
     * @return kết quả thống kê
     */
    @Query(value = """
            SELECT
                COUNT(gc.commit_id)                        AS commitCount,
                ISNULL(SUM(ISNULL(gc.additions,     0)), 0) AS totalAdditions,
                ISNULL(SUM(ISNULL(gc.deletions,     0)), 0) AS totalDeletions,
                ISNULL(SUM(ISNULL(gc.files_changed, 0)), 0) AS totalFilesChanged,
                MAX(gc.commit_date)                        AS latestCommitDate
            FROM GitCommit gc
            JOIN Repository r ON r.repo_id = gc.repo_id
            WHERE r.group_id = :groupId AND gc.author_user_id = :userId
            """,
            nativeQuery = true)
    PersonalCommitStatsProjection getPersonalCommitStats(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId);

    @Query(value = """
        SELECT COUNT(gc.commit_id)
        FROM GitCommit gc
        JOIN Repository r ON r.repo_id = gc.repo_id
        WHERE r.group_id = :groupId
          AND gc.commit_date BETWEEN :fromDate AND :toDate
        """, nativeQuery = true)
    long countCommitsByGroupAndDateRange(
            @Param("groupId") Long groupId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate);

    @Query(value = """
        SELECT COUNT(gc.commit_id)
        FROM GitCommit gc
        JOIN Repository r ON r.repo_id = gc.repo_id
        WHERE r.group_id = :groupId
          AND gc.author_user_id = :userId
          AND gc.commit_date BETWEEN :fromDate AND :toDate
        """, nativeQuery = true)
    long countCommitsByGroupAndUserAndDateRange(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate);

    @Query(value = """
        SELECT MAX(gc.commit_date)
        FROM GitCommit gc
        JOIN Repository r ON r.repo_id = gc.repo_id
        WHERE r.group_id = :groupId
          AND gc.commit_date BETWEEN :fromDate AND :toDate
        """, nativeQuery = true)
    java.time.LocalDateTime findLatestCommitDateByGroup(
            @Param("groupId") Long groupId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate);

    @Query(value = """
        SELECT MAX(gc.commit_date)
        FROM GitCommit gc
        JOIN Repository r ON r.repo_id = gc.repo_id
        WHERE r.group_id = :groupId
          AND gc.author_user_id = :userId
          AND gc.commit_date BETWEEN :fromDate AND :toDate
        """, nativeQuery = true)
    java.time.LocalDateTime findLatestCommitDateByGroupAndUser(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate);
}
