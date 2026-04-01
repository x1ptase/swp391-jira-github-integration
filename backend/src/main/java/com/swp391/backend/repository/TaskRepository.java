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

    List<Task> findAllByParentTask_TaskIdAndJiraIssueType(
            Integer parentTaskId,
            String jiraIssueType,
            Sort sort);

    Optional<Task> findByTaskIdAndStudentGroup_GroupIdAndParentTaskIsNullAndJiraIssueType(
            Integer taskId,
            Long groupId,
            String jiraIssueType);

    // ── My Work – Subtasks (personal view) ───────────────────────────────────

    @Query("""
            SELECT t FROM Task t
            WHERE t.studentGroup.groupId = :groupId
              AND t.jiraIssueType = 'SUBTASK'
              AND t.parentTask IS NOT NULL
              AND t.assignee.userId = :currentUserId
              AND (:statusId IS NULL OR t.status.statusId = :statusId)
              AND (:priority IS NULL OR LOWER(t.jiraPriorityRaw) = LOWER(:priority))
              AND (:requirementId IS NULL OR t.requirement.requirementId = :requirementId)
              AND (:parentTaskId IS NULL OR t.parentTask.taskId = :parentTaskId)
              AND (:keyword IS NULL
                   OR LOWER(t.jiraIssueKey) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(t.title)        LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Task> findMyWorkSubtasks(
            @Param("groupId") Long groupId,
            @Param("currentUserId") Long currentUserId,
            @Param("statusId") Integer statusId,
            @Param("priority") String priority,
            @Param("requirementId") Integer requirementId,
            @Param("parentTaskId") Integer parentTaskId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
            SELECT t FROM Task t
            LEFT JOIN FETCH t.parentTask p
            LEFT JOIN FETCH p.requirement
            LEFT JOIN FETCH t.assignee
            LEFT JOIN FETCH t.status
            WHERE t.taskId = :taskId
              AND t.studentGroup.groupId = :groupId
              AND t.jiraIssueType = 'SUBTASK'
              AND t.assignee.userId = :currentUserId
            """)
    Optional<Task> findMyWorkSubtaskDetail(
            @Param("taskId") Integer taskId,
            @Param("groupId") Long groupId,
            @Param("currentUserId") Long currentUserId);

    @Query(value = """
        SELECT COUNT(*)
        FROM Task t
        JOIN TaskStatus ts ON ts.status_id = t.status_id
        WHERE t.group_id = :groupId
          AND t.due_date < CAST(GETDATE() AS DATE)
          AND UPPER(ts.code) <> 'DONE'
        """, nativeQuery = true)
    long countOverdueTasksByGroupId(@Param("groupId") Long groupId);
}
