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

    /** Preload batch để tránh N+1 khi upsert */
    List<Requirement> findAllByJiraIssueKeyIn(List<String> jiraIssueKeys);

    /**
     * Tìm kiếm Requirement (chỉ EPIC) cho dashboard với filter tuỳ chọn.
     *
     * @param groupId    bắt buộc
     * @param statusId   optional, null = bỏ qua filter
     * @param priorityId optional, null = bỏ qua filter
     * @param keyword    optional, null = bỏ qua filter; tìm trong jiraIssueKey hoặc
     *                   title
     * @param pageable   phân trang
     */
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

    /**
     * Kiểm tra requirementId có thuộc groupId không.
     * Dùng để validate trước khi query story list (404 nếu false).
     *
     * @param requirementId PK của Requirement
     * @param groupId       group phải match
     */
    boolean existsByRequirementIdAndStudentGroup_GroupId(Integer requirementId, Long groupId);

    /**
     * Fetch Requirement theo requirementId + groupId để lấy epicKey/epicSummary.
     * Trả empty nếu requirementId không thuộc groupId.
     *
     * @param requirementId PK của Requirement
     * @param groupId       group phải match
     */
    Optional<Requirement> findByRequirementIdAndStudentGroup_GroupId(Integer requirementId, Long groupId);
}
