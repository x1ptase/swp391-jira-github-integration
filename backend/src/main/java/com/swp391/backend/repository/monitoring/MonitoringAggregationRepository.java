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

    // ── Single-shot monitoring query by classId ───────────────────────────────

    /**
     * Lấy toàn bộ dữ liệu giám sát cho các nhóm trong một lớp học, trong một lần truy vấn duy nhất.
     *
     * <p><b>Chiến lược:</b> Thay vì gọi nhiều batch queries rồi assemble ở tầng Service,
     * query này dùng LEFT JOIN + conditional aggregation (MAX CASE WHEN) để trả về
     * một hàng phẳng (flat row) cho mỗi group. Phù hợp khi cần export hoặc hiển thị
     * bảng tổng quan cho một class cụ thể.
     *
     * <p><b>Lưu ý Active Members:</b> Đếm theo định danh thực tế theo thứ tự ưu tiên:
     * {@code user_id} (đã map) → {@code author_login} → {@code author_email} → {@code author_name}.
     *
     * <p><b>Lưu ý Sync Status:</b> Lấy trạng thái và thời điểm bắt đầu của sync
     * <em>mới nhất</em> cho từng source (GITHUB / JIRA) bằng {@code ROW_NUMBER()} trong CTE.
     *
     * @param classId  ID của lớp học cần giám sát
     * @param fromDate Bắt đầu khoảng thời gian lọc commit (inclusive)
     * @param toDate   Kết thúc khoảng thời gian lọc commit (inclusive)
     * @return danh sách {@link GroupRawProjection}, mỗi phần tử tương ứng một group
     */
    @Query(value = """
            WITH LatestSync AS (
                SELECT group_id,
                       source,
                       status,
                       started_at,
                       ROW_NUMBER() OVER (PARTITION BY group_id, source ORDER BY started_at DESC) AS rn
                FROM SyncLog
            )
            SELECT
                sg.group_id          AS groupId,
                sg.group_name        AS groupName,
                sg.status            AS groupStatus,
                tp.topic_name        AS topicName,

                -- Member count
                COUNT(DISTINCT gm.user_id)          AS totalMembers,

                -- Active members: tính theo định danh thực tế
                COUNT(DISTINCT
                    CASE
                        WHEN gc_active.author_user_id IS NOT NULL
                            THEN CAST('uid:' + CAST(gc_active.author_user_id AS NVARCHAR(20)) AS NVARCHAR(200))
                        WHEN gc_active.author_login IS NOT NULL
                            THEN CAST('login:' + gc_active.author_login AS NVARCHAR(200))
                        WHEN gc_active.author_email IS NOT NULL
                            THEN CAST('email:' + gc_active.author_email AS NVARCHAR(200))
                        ELSE CAST('name:' + ISNULL(gc_active.author_name, '(unknown)') AS NVARCHAR(200))
                    END
                )                                   AS activeMembers,

                -- Overdue tasks: due_date đã qua hiện tại và chưa DONE
                SUM(CASE
                    WHEN t.due_date IS NOT NULL
                         AND CAST(t.due_date AS DATETIME2) < GETUTCDATE()
                         AND UPPER(ts.code) != 'DONE'
                    THEN 1 ELSE 0
                END)                                AS overdueTasks,

                -- Commit stats trong khoảng thời gian
                COUNT(DISTINCT gc_active.commit_id) AS totalCommits,
                MAX(gc_active.commit_date)          AS lastCommitAt,

                -- Sync status: GitHub
                MAX(CASE WHEN ls_gh.source = 'GITHUB' THEN ls_gh.status      END) AS githubSyncStatus,
                MAX(CASE WHEN ls_gh.source = 'GITHUB' THEN ls_gh.started_at  END) AS githubSyncStartedAt,

                -- Sync status: Jira
                MAX(CASE WHEN ls_ji.source = 'JIRA'   THEN ls_ji.status      END) AS jiraSyncStatus,
                MAX(CASE WHEN ls_ji.source = 'JIRA'   THEN ls_ji.started_at  END) AS jiraSyncStartedAt

            FROM StudentGroup sg

            -- Topic (optional)
            LEFT JOIN Topic tp
                ON tp.topic_id = sg.topic_id

            -- All members
            LEFT JOIN GroupMember gm
                ON gm.group_id = sg.group_id

            -- Repositories của nhóm
            LEFT JOIN Repository r
                ON r.group_id = sg.group_id

            -- Commits trong khoảng thời gian (dùng cho totalCommits, lastCommitAt, activeMembers)
            LEFT JOIN GitCommit gc_active
                ON gc_active.repo_id = r.repo_id
               AND gc_active.commit_date BETWEEN :fromDate AND :toDate

            -- Tasks của nhóm (để tính overdue)
            LEFT JOIN Task t
                ON t.group_id = sg.group_id

            -- TaskStatus
            LEFT JOIN TaskStatus ts
                ON ts.status_id = t.status_id

            -- Sync mới nhất: GitHub
            LEFT JOIN LatestSync ls_gh
                ON ls_gh.group_id = sg.group_id
               AND ls_gh.source   = 'GITHUB'
               AND ls_gh.rn       = 1

            -- Sync mới nhất: JIRA
            LEFT JOIN LatestSync ls_ji
                ON ls_ji.group_id = sg.group_id
               AND ls_ji.source   = 'JIRA'
               AND ls_ji.rn       = 1

            WHERE sg.class_id = :classId

            GROUP BY
                sg.group_id,
                sg.group_name,
                sg.status,
                tp.topic_name
            """, nativeQuery = true)
    List<GroupRawProjection> getGroupRawByClassId(
            @Param("classId") Long classId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate);
    // ── Single-shot student watchlist query by classId ────────────────────────

    /**
     * Lấy danh sách Watchlist sinh viên cho toàn bộ lớp học trong một lần truy vấn duy nhất.
     *
     * <h4>Chiến lược JOIN:</h4>
     * <ul>
     *   <li><b>Base list</b>: {@code StudentClassAssignment} → đảm bảo toàn bộ sinh viên
     *       trong lớp đều xuất hiện, kể cả người chưa có nhóm/commit.</li>
     *   <li><b>LEFT JOIN GroupMember + StudentGroup</b>: lấy thông tin nhóm và vai trò.
     *       Sinh viên chưa vào nhóm sẽ có {@code groupId, groupName, memberRole = null}.</li>
     *   <li><b>LEFT JOIN Repository + GitCommit</b>: tính commit trong khoảng thời gian.
     *       Phải qua nhóm → repository → commit để đảm bảo đúng scope;
     *       sinh viên chưa có commit sẽ có {@code commitCount = 0}.</li>
     * </ul>
     *
     * <h4>Match commit với sinh viên:</h4>
     * <p>Commit được tính cho sinh viên khi {@code gc.author_user_id = u.user_id}
     * (đã được map trong quá trình sync GitHub). Sinh viên chưa map GitHub email
     * vào tài khoản hệ thống sẽ không được tính commit dù có commit trên GitHub.
     *
     * @param classId  ID lớp học
     * @param fromDate Bắt đầu khoảng thời gian lọc commit (inclusive)
     * @param toDate   Kết thúc khoảng thời gian lọc commit (inclusive)
     * @return danh sách {@link StudentWatchlistProjection}, mỗi phần tử là một sinh viên
     */
    @Query(value = """
            SELECT
                u.user_id                   AS userId,
                u.full_name                 AS fullName,
                u.student_code              AS studentCode,
                u.email                     AS email,

                -- Thông tin nhóm (null nếu sinh viên chưa vào nhóm nào)
                sg.group_id                 AS groupId,
                sg.group_name               AS groupName,
                mr.code                     AS memberRole,

                -- Commit count (0 nếu không có commit, không bao giờ null)
                ISNULL(COUNT(DISTINCT gc.commit_id), 0) AS commitCount,

                -- Commit gần nhất trong khoảng thời gian
                MAX(gc.commit_date)         AS lastActiveAt

            FROM StudentClassAssignment sca

            -- Lấy thông tin sinh viên
            JOIN Users u
                ON u.user_id = sca.student_id

            -- LEFT JOIN vào nhóm: sinh viên chưa vào nhóm vẫn hiện ra
            LEFT JOIN GroupMember gm
                ON gm.user_id  = u.user_id

            LEFT JOIN StudentGroup sg
                ON sg.group_id  = gm.group_id
               AND sg.class_id  = sca.class_id

            -- LEFT JOIN lấy role trong nhóm
            LEFT JOIN MemberRole mr
                ON mr.member_role_id = gm.member_role_id

            -- LEFT JOIN qua Repository để lọc đúng repo của nhóm trong lớp
            LEFT JOIN Repository r
                ON r.group_id = sg.group_id

            -- LEFT JOIN lấy commit của sinh viên trong khoảng thời gian
            -- Quan trọng: match qua author_user_id để đúng người, không bị lẫn commit người khác
            LEFT JOIN GitCommit gc
                ON gc.repo_id           = r.repo_id
               AND gc.author_user_id    = u.user_id
               AND gc.commit_date BETWEEN :fromDate AND :toDate

            WHERE sca.class_id = :classId

            GROUP BY
                u.user_id,
                u.full_name,
                u.student_code,
                u.email,
                sg.group_id,
                sg.group_name,
                mr.code

            ORDER BY
                ISNULL(COUNT(DISTINCT gc.commit_id), 0) ASC,
                u.full_name ASC
            """, nativeQuery = true)
    List<StudentWatchlistProjection> getStudentWatchlistByClassId(
            @Param("classId") Long classId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate);
}
