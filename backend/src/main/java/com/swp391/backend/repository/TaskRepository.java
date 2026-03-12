package com.swp391.backend.repository;

import com.swp391.backend.dto.response.StoryCountProjection;
import com.swp391.backend.dto.response.StoryStatusCountProjection;
import com.swp391.backend.dto.response.SubtaskCountProjection;
import com.swp391.backend.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

    Optional<Task> findByJiraIssueKey(String jiraIssueKey);

    /** Preload batch để tránh N+1 khi upsert */
    List<Task> findAllByJiraIssueKeyIn(List<String> jiraIssueKeys);

    // ── Requirement dashboard (epic list) ─────────────────────────────────────

    /**
     * Đếm số Story (jira_issue_type = 'STORY') theo từng requirementId.
     * Batch query tránh N+1 khi build dashboard response.
     *
     * @param requirementIds danh sách requirement PK
     * @return list projection {requirementId, count}
     */
    @Query("""
            SELECT t.requirement.requirementId AS requirementId,
                   COUNT(t) AS count
            FROM Task t
            WHERE t.requirement.requirementId IN :requirementIds
              AND t.jiraIssueType = 'STORY'
            GROUP BY t.requirement.requirementId
            """)
    List<StoryCountProjection> countStoriesByRequirementIds(
            @Param("requirementIds") List<Integer> requirementIds);

    /**
     * Đếm số Story có status DONE theo từng requirementId.
     * Batch query tránh N+1 khi build dashboard response.
     *
     * @param requirementIds danh sách requirement PK
     * @return list projection {requirementId, count}
     */
    @Query("""
            SELECT t.requirement.requirementId AS requirementId,
                   COUNT(t) AS count
            FROM Task t
            WHERE t.requirement.requirementId IN :requirementIds
              AND t.jiraIssueType = 'STORY'
              AND UPPER(t.status.code) = 'DONE'
            GROUP BY t.requirement.requirementId
            """)
    List<StoryCountProjection> countDoneStoriesByRequirementIds(
            @Param("requirementIds") List<Integer> requirementIds);

    // ── Story dashboard ────────────────────────────────────────────────────────

    /**
     * Lấy danh sách Story (STORY top-level) thuộc một Requirement + Group
     * với các filter tuỳ chọn. Phân trang qua {@code Pageable}.
     *
     * <p>Không dùng JOIN FETCH để tránh lỗi count/duplicate khi pageable.
     * Sort nên được cung cấp qua Pageable (không dùng NULLS LAST trong JPQL).
     *
     * @param requirementId bắt buộc
     * @param groupId       bắt buộc
     * @param statusId      optional — null = bỏ qua
     * @param assigneeId    optional — null = bỏ qua; nếu myTasks=true caller
     *                      truyền currentUserId vào đây
     * @param keyword       optional — null = bỏ qua; tìm trong jiraIssueKey
     *                      hoặc title (case-insensitive)
     * @param pageable      phân trang + sort
     * @return page of Task entity (Story)
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.requirement.requirementId = :requirementId
              AND t.studentGroup.groupId = :groupId
              AND t.parentTask IS NULL
              AND t.jiraIssueType = 'STORY'
              AND (:statusId IS NULL OR t.status.statusId = :statusId)
              AND (:assigneeId IS NULL OR t.assignee.userId = :assigneeId)
              AND (:keyword IS NULL
                   OR LOWER(t.jiraIssueKey) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(t.title)        LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Task> findStoriesByRequirementAndGroup(
            @Param("requirementId") Integer requirementId,
            @Param("groupId") Long groupId,
            @Param("statusId") Integer statusId,
            @Param("assigneeId") Long assigneeId,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * Batch-count subtask theo parentTaskId để tránh N+1.
     *
     * @param parentTaskIds danh sách taskId của Story
     * @return list projection {parentTaskId, count}
     */
    @Query("""
            SELECT t.parentTask.taskId AS parentTaskId,
                   COUNT(t) AS count
            FROM Task t
            WHERE t.parentTask.taskId IN :parentTaskIds
              AND t.jiraIssueType = 'SUBTASK'
            GROUP BY t.parentTask.taskId
            """)
    List<SubtaskCountProjection> countSubtasksByParentTaskIds(
            @Param("parentTaskIds") List<Integer> parentTaskIds);

    /**
     * Aggregate story count theo status để build progress summary.
     * Áp dụng đúng cùng dataset filter với findStoriesByRequirementAndGroup.
     * doneCount sẽ được tính từ kết quả này theo {@code UPPER(statusCode) = 'DONE'}.
     *
     * @param requirementId bắt buộc
     * @param groupId       bắt buộc
     * @param statusId      optional — null = bỏ qua (cùng filter với query chính)
     * @param assigneeId    optional — null = bỏ qua (cùng filter với query chính)
     * @param keyword       optional — null = bỏ qua (cùng filter với query chính)
     * @return list projection {statusId, statusCode, count}
     */
    @Query("""
            SELECT t.status.statusId AS statusId,
                   t.status.code     AS statusCode,
                   COUNT(t)          AS count
            FROM Task t
            WHERE t.requirement.requirementId = :requirementId
              AND t.studentGroup.groupId = :groupId
              AND t.parentTask IS NULL
              AND t.jiraIssueType = 'STORY'
              AND (:statusId IS NULL OR t.status.statusId = :statusId)
              AND (:assigneeId IS NULL OR t.assignee.userId = :assigneeId)
              AND (:keyword IS NULL
                   OR LOWER(t.jiraIssueKey) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(t.title)        LIKE LOWER(CONCAT('%', :keyword, '%')))
            GROUP BY t.status.statusId, t.status.code
            """)
    List<StoryStatusCountProjection> countStoriesByStatusForRequirement(
            @Param("requirementId") Integer requirementId,
            @Param("groupId") Long groupId,
            @Param("statusId") Integer statusId,
            @Param("assigneeId") Long assigneeId,
            @Param("keyword") String keyword);

    // ── Subtask list ──────────────────────────────────────────────────────────

    /**
     * Lấy danh sách Subtask theo parentStoryId.
     * Sort được cung cấp qua tham số {@code Sort} (không dùng NULLS LAST trong JPQL).
     *
     * @param parentTaskId taskId của Story cha
     * @param sort         yêu cầu sort (caller: jiraUpdatedAt desc + createdAt desc)
     * @return list Task entity (Subtask)
     */
    List<Task> findAllByParentTask_TaskIdAndJiraIssueType(
            Integer parentTaskId,
            String jiraIssueType,
            Sort sort);

    /**
     * Validate storyId hợp lệ: thuộc groupId, là top-level STORY.
     * Dùng để 404-check trước khi query subtask.
     *
     * @param taskId       PK của Task
     * @param groupId      group phải match
     * @param jiraIssueType phải là 'STORY'
     * @return Optional Task nếu tìm thấy
     */
    Optional<Task> findByTaskIdAndStudentGroup_GroupIdAndParentTaskIsNullAndJiraIssueType(
            Integer taskId,
            Long groupId,
            String jiraIssueType);
}
