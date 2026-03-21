package com.swp391.backend.controller;

import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.dto.response.RequirementDashboardItemResponse;
import com.swp391.backend.service.RequirementQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các endpoint liên quan đến Requirement (Epic).
 * Dashboard: lấy requirement list của group theo permission.
 *
 * <p>
 * Permission rules (enforce tại @PreAuthorize):
 * <ul>
 * <li>ADMIN: xem được mọi group</li>
 * <li>LECTURER: chỉ xem group được assign trong LecturerAssignment</li>
 * <li>STUDENT (Leader/Member): chỉ xem group mình thuộc</li>
 * </ul>
 * Dùng {@code @securityService.hasAccessToGroup(#groupId)} — đã cover đủ 3
 * roles trên.
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementQueryService requirementQueryService;

    /**
     * Lấy danh sách Requirement (Jira Epic) của group cho dashboard.
     *
     * <p>
     * AC đáp ứng:
     * <ul>
     * <li>1. Admin xem mọi group</li>
     * <li>2. Lecturer chỉ xem group được assign</li>
     * <li>3. Leader/Student chỉ xem group mình thuộc</li>
     * <li>4–5. Chỉ trả Epic (jira_issue_type='EPIC'), không có Story/Subtask</li>
     * <li>6. Filter theo statusId, priorityId, keyword</li>
     * <li>7–8. Response có đủ: epicKey, summary, status, priority, updated, stories
     * progress</li>
     * <li>9. Group không có Epic → 200 + page rỗng</li>
     * <li>10. Không có quyền → 403 (AccessDeniedException từ @PreAuthorize)</li>
     * </ul>
     *
     * @param groupId    group ID (path variable)
     * @param statusId   optional filter theo internal RequirementStatus.statusId
     * @param priorityId optional filter theo internal Priority.priorityId
     * @param keyword    optional search theo jiraIssueKey hoặc title
     *                   (case-insensitive)
     * @param page       trang, default 0
     * @param size       kích thước trang, default 20
     * @return page response bọc trong ApiResponse
     */
    @GetMapping("/{groupId}/requirements")
    @PreAuthorize("@securityService.hasAccessToGroup(#groupId)")
    public ApiResponse<Page<RequirementDashboardItemResponse>> listRequirements(
            @PathVariable Long groupId,
            @RequestParam(value = "statusId", required = false) Integer statusId,
            @RequestParam(value = "priorityId", required = false) Integer priorityId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        // Default sort: jiraUpdatedAt desc (nulls last), fallback createdAt desc
        Sort sort = Sort.by(
                Sort.Order.desc("jiraUpdatedAt"),
                Sort.Order.desc("createdAt"));

        PageRequest pageable = PageRequest.of(page, size, sort);

        Page<RequirementDashboardItemResponse> result = requirementQueryService
                .getDashboardRequirements(groupId, statusId, priorityId, keyword, pageable);

        return ApiResponse.success(result);
    }
}
