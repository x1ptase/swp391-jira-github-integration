package com.swp391.backend.service;

import com.swp391.backend.dto.response.RequirementDashboardItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service để query Requirement cho dashboard view by group.
 * Chỉ trả về Requirement có jira_issue_type = 'EPIC'.
 */
public interface RequirementQueryService {

    /**
     * Lấy danh sách Requirement (Epic) của group với filter và pagination.
     * Permission phải được kiểm tra ở controller layer bằng @PreAuthorize.
     *
     * @param groupId    ID của group (bắt buộc)
     * @param statusId   filter theo internal status id (optional, null = bỏ qua)
     * @param priorityId filter theo internal priority id (optional, null = bỏ qua)
     * @param keyword    search theo jiraIssueKey hoặc title (optional, null = bỏ
     *                   qua)
     * @param pageable   phân trang
     * @return page response DTO với story progress đã được tính
     */
    Page<RequirementDashboardItemResponse> getDashboardRequirements(
            Long groupId,
            Integer statusId,
            Integer priorityId,
            String keyword,
            Pageable pageable);
}
