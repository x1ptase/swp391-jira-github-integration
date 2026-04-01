package com.swp391.backend.repository;

import com.swp391.backend.entity.Requirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, Integer> {

    Optional<Requirement> findByJiraIssueKey(String jiraIssueKey);

    List<Requirement> findAllByJiraIssueKeyIn(List<String> jiraIssueKeys);

    @Query("""
            SELECT r FROM Requirement r
            WHERE r.studentGroup.groupId = :groupId
              AND r.jiraIssueType = 'EPIC'
              AND (:statusId IS NULL OR r.status.statusId = :statusId)
              AND (:priorityId IS NULL OR r.priority.priorityId = :priorityId)
              AND (:keyword IS NULL
                   OR LOWER(r.jiraIssueKey) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(r.title)        LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Requirement> searchDashboardRequirements(
            @Param("groupId") Long groupId,
            @Param("statusId") Integer statusId,
            @Param("priorityId") Integer priorityId,
            @Param("keyword") String keyword,
            Pageable pageable);

    // ── Story dashboard helpers ───────────────────────────────────────────────

    boolean existsByRequirementIdAndStudentGroup_GroupId(Integer requirementId, Long groupId);

    Optional<Requirement> findByRequirementIdAndStudentGroup_GroupId(Integer requirementId, Long groupId);
}
