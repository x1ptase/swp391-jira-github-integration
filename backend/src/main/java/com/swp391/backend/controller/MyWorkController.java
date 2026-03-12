package com.swp391.backend.controller;

import com.swp391.backend.common.ApiResponse;
import com.swp391.backend.dto.response.MyWorkSubtaskDetailResponse;
import com.swp391.backend.dto.response.MyWorkSubtaskListResponse;
import com.swp391.backend.service.MyWorkQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý màn hình "My Work – Subtasks" dành cho Team Member.
 *
 * <p>Permission rule (enforce tại @PreAuthorize):
 * <ul>
 *   <li>Chỉ STUDENT thuộc group mới được gọi các endpoint này</li>
 *   <li>LECTURER và ADMIN không phải đối tượng của màn này → 403</li>
 * </ul>
 * Dùng {@code @securityService.isStudentInGroup(#groupId)} — đã có sẵn trong SecurityService.
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class MyWorkController {

    private final MyWorkQueryService myWorkQueryService;

    /**
     * Lấy danh sách Subtask được assign cho current user trong group.
     *
     * <p>AC đáp ứng:
     * <ul>
     *   <li>1. Chỉ hiển thị issue type = Subtask</li>
     *   <li>2. Chỉ assign cho current user</li>
     *   <li>3. Assignment theo mapping internal (assignee.userId)</li>
     *   <li>4. Nếu chưa map jiraAccountId → mappedToJira=false, FE hiển thị unmapped state</li>
     *   <li>5. Mỗi item có: subtaskKey, summary, status, priority, parent story, epic, updated</li>
     *   <li>6. Filter: status, priority, epic, story, keyword</li>
     *   <li>7. Không có subtask → 200 + page rỗng</li>
     *   <li>9. Không thuộc group → 403</li>
     * </ul>
     *
     * @param groupId       group ID (path variable)
     * @param statusId      optional filter theo internal TaskStatus.statusId
     * @param priority      optional filter theo raw priority string (case-insensitive)
     * @param requirementId optional filter theo Epic/Requirement ID
     * @param storyId       optional filter theo parent Story taskId
     * @param keyword       optional search trong jiraIssueKey hoặc title (case-insensitive)
     * @param page          trang, default 0
     * @param size          kích thước trang, default 20
     * @return MyWorkSubtaskListResponse bọc trong ApiResponse
     */
    @GetMapping("/{groupId}/my-work/subtasks")
    @PreAuthorize("@securityService.isStudentInGroup(#groupId)")
    public ApiResponse<MyWorkSubtaskListResponse> listMyWorkSubtasks(
            @PathVariable Long groupId,
            @RequestParam(value = "statusId", required = false) Integer statusId,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "requirementId", required = false) Integer requirementId,
            @RequestParam(value = "storyId", required = false) Integer storyId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        // Default sort: jiraUpdatedAt desc (nulls last), fallback createdAt desc
        Sort sort = Sort.by(
                Sort.Order.desc("jiraUpdatedAt"),
                Sort.Order.desc("createdAt"));

        PageRequest pageable = PageRequest.of(page, size, sort);

        MyWorkSubtaskListResponse result = myWorkQueryService.getMyWorkSubtasks(
                groupId, statusId, priority, requirementId, storyId, keyword, pageable);

        return ApiResponse.success(result);
    }

    /**
     * Lấy detail một Subtask của current user.
     *
     * <p>AC đáp ứng:
     * <ul>
     *   <li>8. Detail có đủ context Story cha + Epic cha</li>
     *   <li>Chỉ cho phép xem subtask của chính mình (leak-safe)</li>
     *   <li>Nếu không tìm thấy hoặc không phải của current user → 404</li>
     *   <li>9. Không thuộc group → 403</li>
     * </ul>
     *
     * @param groupId group ID (path variable)
     * @param taskId  PK của Subtask Task
     * @return MyWorkSubtaskDetailResponse bọc trong ApiResponse
     */
    @GetMapping("/{groupId}/my-work/subtasks/{taskId}")
    @PreAuthorize("@securityService.isStudentInGroup(#groupId)")
    public ApiResponse<MyWorkSubtaskDetailResponse> getMyWorkSubtaskDetail(
            @PathVariable Long groupId,
            @PathVariable Integer taskId) {

        MyWorkSubtaskDetailResponse result = myWorkQueryService
                .getMyWorkSubtaskDetail(groupId, taskId);

        return ApiResponse.success(result);
    }
}
