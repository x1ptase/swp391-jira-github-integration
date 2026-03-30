package com.swp391.backend.repository.monitoring;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository tổng hợp dữ liệu cho module monitoring.
 * <p>
 * <b>Chiến lược tránh N+1:</b> Tất cả các query đều là batch queries,
 * nhận vào danh sách groupIds và trả về kết quả tổng hợp theo group.
 * Caller chịu trách nhiệm chuyển kết quả thành Map&lt;groupId, value&gt;.
 *
 * <p><b>Native SQL (SQL Server):</b> Một số query dùng native SQL để
 * tận dụng các tính năng SQL Server không hỗ trợ trong JPQL.
 */
@Repository
public interface MonitoringAggregationRepository
        extends org.springframework.data.jpa.repository.JpaRepository<com.swp391.backend.entity.StudentGroup, Long> {

    // ── Groups by class ───────────────────────────────────────────────────────

    /**
     * Lấy tất cả nhóm OPEN theo danh sách classId.
     * Dùng để load batch khi hiển thị danh sách lớp.
     *
     * @param classIds danh sách classId
     * @return danh sách StudentGroup với status = 'OPEN'
     */
    @Query("SELECT sg FROM StudentGroup sg WHERE sg.academicClass.classId IN :classIds AND sg.status = 'OPEN'")
    List<com.swp391.backend.entity.StudentGroup> findOpenGroupsByClassIds(@Param("classIds") List<Long> classIds);

    // ── Member count by group (batch) ─────────────────────────────────────────

    /**
     * Đếm số thành viên theo từng nhóm (batch).
     * <p>
     * Tránh N+1 khi cần member count của nhiều nhóm cùng lúc.
     *
     * @param groupIds danh sách groupId
     * @return projection {groupId, totalMembers}
     */
    @Query(value = """
            SELECT gm.group_id AS groupId,
                   COUNT(gm.user_id) AS totalMembers
            FROM GroupMember gm
            WHERE gm.group_id IN :groupIds
            GROUP BY gm.group_id
            """, nativeQuery = true)
    List<GroupMemberCountProjection> countMembersByGroupIds(@Param("groupIds") List<Long> groupIds);

    // ── Active member count by group in time window (batch) ───────────────────

    /**
     * Đếm số thành viên đã có ít nhất 1 commit trong khoảng thời gian, theo nhóm.
     * <p>
     * "Active member" = author_user_id đã được map (không null) VÀ có commit
     * trong khoảng [fromDate, toDate].
     *
     * @param groupIds danh sách groupId
     * @param fromDate bắt đầu khoảng thời gian (inclusive)
     * @param toDate   kết thúc khoảng thời gian (inclusive)
     * @return projection {groupId, activeMembers}
     */
    @Query(value = """
            SELECT r.group_id AS groupId,
                   COUNT(DISTINCT 
                     CASE 
                       WHEN gc.author_user_id IS NOT NULL THEN CAST('uid:' + CAST(gc.author_user_id AS NVARCHAR(20)) AS NVARCHAR(200))
                       WHEN gc.author_login IS NOT NULL THEN CAST('login:' + gc.author_login AS NVARCHAR(200))
                       WHEN gc.author_email IS NOT NULL THEN CAST('email:' + gc.author_email AS NVARCHAR(200))
                       ELSE CAST('name:' + ISNULL(gc.author_name, '(unknown)') AS NVARCHAR(200))
                     END
                   ) AS activeMembers
            FROM GitCommit gc
            JOIN Repository r ON r.repo_id = gc.repo_id
            WHERE r.group_id IN :groupIds
              AND gc.commit_date BETWEEN :fromDate AND :toDate
            GROUP BY r.group_id
            """, nativeQuery = true)
    List<GroupActiveMemberCountProjection> countActiveMembersByGroupIds(
            @Param("groupIds") List<Long> groupIds,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    // ── Commit count by group in time window (batch) ──────────────────────────

    /**
     * Đếm tổng commit và lấy thời điểm commit gần nhất theo từng nhóm trong khoảng thời gian.
     * <p>
     * Batch query tránh N+1.
     *
     * @param groupIds danh sách groupId
     * @param fromDate bắt đầu khoảng thời gian (inclusive)
     * @param toDate   kết thúc khoảng thời gian (inclusive)
     * @return projection {groupId, totalCommits, lastCommitAt}
     */
    @Query(value = """
            SELECT r.group_id AS groupId,
                   COUNT(gc.commit_id) AS totalCommits,
                   MAX(gc.commit_date) AS lastCommitAt
            FROM GitCommit gc
            JOIN Repository r ON r.repo_id = gc.repo_id
            WHERE r.group_id IN :groupIds
              AND gc.commit_date BETWEEN :fromDate AND :toDate
            GROUP BY r.group_id
            """, nativeQuery = true)
    List<GroupCommitSummaryProjection> getCommitSummaryByGroupIds(
            @Param("groupIds") List<Long> groupIds,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    // ── Commit count by user in a group in time window (batch) ────────────────

    /**
     * Thống kê commit theo từng user trong nhiều nhóm, trong khoảng thời gian.
     * <p>
     * Chỉ tính cho author_user_id đã được map (không null).
     * Dùng cho tính topContributorShare và detection UNEVEN_CONTRIBUTION.
     *
     * @param groupIds danh sách groupId
     * @param fromDate bắt đầu khoảng thời gian
     * @param toDate   kết thúc khoảng thời gian
     * @return projection {groupId, userId, commitCount, lastCommitAt}
     */
    @Query(value = """
            SELECT r.group_id AS groupId,
                   MAX(gc.author_user_id) AS userId,
                   COUNT(gc.commit_id) AS commitCount,
                   MAX(gc.commit_date) AS lastCommitAt
            FROM GitCommit gc
            JOIN Repository r ON r.repo_id = gc.repo_id
            WHERE r.group_id IN :groupIds
              AND gc.commit_date BETWEEN :fromDate AND :toDate
            GROUP BY r.group_id, 
                     CASE 
                       WHEN gc.author_user_id IS NOT NULL THEN CAST('uid:' + CAST(gc.author_user_id AS NVARCHAR(20)) AS NVARCHAR(200))
                       WHEN gc.author_login IS NOT NULL THEN CAST('login:' + gc.author_login AS NVARCHAR(200))
                       WHEN gc.author_email IS NOT NULL THEN CAST('email:' + gc.author_email AS NVARCHAR(200))
                       ELSE CAST('name:' + ISNULL(gc.author_name, '(unknown)') AS NVARCHAR(200))
                     END
            """, nativeQuery = true)
    List<UserGroupCommitProjection> getUserCommitsByGroupIds(
            @Param("groupIds") List<Long> groupIds,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    // ── Overdue task count by group (batch) ───────────────────────────────────

    /**
     * Đếm số task overdue theo nhóm.
     * <p>
     * "Overdue" = {@code due_date} không null, đã qua hiện tại,
     * VÀ {@code status.code} không phải 'DONE' (case-insensitive).
     *
     * @param groupIds danh sách groupId
     * @param now      thời điểm hiện tại để so sánh
     * @return projection {groupId, overdueTasks}
     */
    @Query(value = """
            SELECT t.group_id AS groupId,
                   COUNT(t.task_id) AS overdueTasks
            FROM Task t
            JOIN TaskStatus ts ON ts.status_id = t.status_id
            WHERE t.group_id IN :groupIds
              AND t.due_date IS NOT NULL
              AND CAST(t.due_date AS DATETIME2) < :now
              AND UPPER(ts.code) != 'DONE'
            GROUP BY t.group_id
            """, nativeQuery = true)
    List<GroupOverdueTaskCountProjection> countOverdueTasksByGroupIds(
            @Param("groupIds") List<Long> groupIds,
            @Param("now") LocalDateTime now);

    // ── Latest sync by group and source (batch) ───────────────────────────────

    /**
     * Lấy thời điểm sync thành công gần nhất theo nhóm và source.
     * <p>
     * Chỉ lấy các sync có {@code status = 'SUCCESS'}.
     * Batch query cho nhiều nhóm cùng lúc.
     *
     * @param groupIds danh sách groupId
     * @return projection {groupId, source, lastSyncAt}
     */
    @Query(value = """
            SELECT sl.group_id AS groupId,
                   sl.source AS source,
                   MAX(sl.ended_at) AS lastSyncAt
            FROM SyncLog sl
            WHERE sl.group_id IN :groupIds
              AND sl.status = 'SUCCESS'
            GROUP BY sl.group_id, sl.source
            """, nativeQuery = true)
    List<GroupSyncStatusProjection> getLastSuccessfulSyncByGroupIds(@Param("groupIds") List<Long> groupIds);

    // ── Topic existence by group ──────────────────────────────────────────────

    /**
     * Lấy danh sách groupId có topic (topic_id IS NOT NULL) từ tập groupIds.
     * <p>
     * Dùng để batch-check topic existence thay vì check từng group.
     *
     * @param groupIds danh sách groupId cần check
     * @return subset groupIds có topic
     */
    @Query("SELECT sg.groupId FROM StudentGroup sg WHERE sg.groupId IN :groupIds AND sg.topic IS NOT NULL")
    List<Long> findGroupIdsWithTopic(@Param("groupIds") List<Long> groupIds);
}
