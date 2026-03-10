package com.swp391.backend.repository;

import com.swp391.backend.dto.response.StoryCountProjection;
import com.swp391.backend.entity.Task;
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
}
