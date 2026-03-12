package com.swp391.backend.controller;

import com.swp391.backend.common.ApiResponse;
import com.swp391.backend.dto.response.StoryListByRequirementResponse;
import com.swp391.backend.dto.response.SubtaskItemResponse;
import com.swp391.backend.service.StoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các endpoint dashboard Story + Subtask.
 *
 * <p>Permission rules (enforce tại @PreAuthorize):
 * <ul>
 *   <li>ADMIN: xem được mọi group</li>
 *   <li>LECTURER: chỉ xem group được assign trong LecturerAssignment</li>
 *   <li>STUDENT (Leader/Member): chỉ xem group mình thuộc</li>
 * </ul>
 * Dùng {@code @securityService.hasAccessToGroup(#groupId)} — cover đủ 3 roles.
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class StoryController {

    private final StoryQueryService storyQueryService;

    /**
     * Lấy danh sách Story (Jira Story) thuộc một Epic + progress summary.
     *
     * <p>AC đáp ứng:
     * <ul>
     *   <li>1–3. Permission theo role (ADMIN/LECTURER/STUDENT)</li>
     *   <li>4. Chỉ Story top-level (parentTask IS NULL, jiraIssueType='STORY')</li>
     *   <li>5. Chỉ Story thuộc Epic đang chọn</li>
     *   <li>6. Filter: status, assignee, keyword, myTasks</li>
     *   <li>7–8. Progress summary theo status của Story</li>
     *   <li>9–10. Story DTO đầy đủ field + subtasksCount</li>
     *   <li>13. Epic không có Story → 200 + empty page</li>
     *   <li>15. Không có quyền → 403</li>
     * </ul>
     *
     * @param groupId       group ID (path variable)
     * @param requirementId Requirement (Epic) ID — phải thuộc groupId
     * @param statusId      optional filter theo internal TaskStatus.statusId
     * @param assigneeId    optional filter theo internal User.userId (ignored nếu myTasks=true)
     * @param keyword       optional search trong jiraIssueKey hoặc summary
     * @param myTasks       nếu true chỉ trả task của current user (default false)
     * @param page          trang, default 0
     * @param size          kích thước trang, default 20
     * @return StoryListByRequirementResponse bọc trong ApiResponse
     */
    @GetMapping("/{groupId}/requirements/{requirementId}/stories")
    @PreAuthorize("@securityService.hasAccessToGroup(#groupId)")
    public ApiResponse<StoryListByRequirementResponse> listStories(
            @PathVariable Long groupId,
            @PathVariable Integer requirementId,
            @RequestParam(value = "statusId", required = false) Integer statusId,
            @RequestParam(value = "assigneeId", required = false) Long assigneeId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "myTasks", defaultValue = "false") boolean myTasks,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        // Default sort: jiraUpdatedAt desc, fallback createdAt desc
        Sort sort = Sort.by(
                Sort.Order.desc("jiraUpdatedAt"),
                Sort.Order.desc("createdAt"));

        PageRequest pageable = PageRequest.of(page, size, sort);

        StoryListByRequirementResponse result = storyQueryService.getStoriesByRequirement(
                groupId, requirementId, statusId, assigneeId, keyword, myTasks, pageable);

        return ApiResponse.success(result);
    }

    /**
     * Lấy danh sách Subtask của một Story.
     *
     * <p>AC đáp ứng:
     * <ul>
     *   <li>1–3. Permission theo role (ADMIN/LECTURER/STUDENT)</li>
     *   <li>11–12. User expand Story → danh sách Subtask với đủ field</li>
     *   <li>14. Story không có Subtask → 200 + list rỗng</li>
     *   <li>15. Không có quyền → 403</li>
     * </ul>
     *
     * @param groupId group ID (path variable)
     * @param storyId taskId của Story — phải là top-level STORY thuộc groupId
     * @return list SubtaskItemResponse bọc trong ApiResponse
     */
    @GetMapping("/{groupId}/stories/{storyId}/subtasks")
    @PreAuthorize("@securityService.hasAccessToGroup(#groupId)")
    public ApiResponse<List<SubtaskItemResponse>> listSubtasks(
            @PathVariable Long groupId,
            @PathVariable Integer storyId) {

        List<SubtaskItemResponse> result = storyQueryService.getSubtasksByStory(groupId, storyId);

        return ApiResponse.success(result);
    }
}
