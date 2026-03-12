package com.swp391.backend.service;

import com.swp391.backend.dto.response.StoryListByRequirementResponse;
import com.swp391.backend.dto.response.SubtaskItemResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service để query Story và Subtask cho dashboard view.
 * Permission phải được kiểm tra ở controller layer bằng @PreAuthorize.
 */
public interface StoryQueryService {

    /**
     * Lấy danh sách Story (STORY top-level) thuộc một Epic + progress summary.
     *
     * <p>Rules:
     * <ul>
     *   <li>requirementId phải thuộc groupId → 404 nếu không match</li>
     *   <li>Chỉ trả Task có: parentTask IS NULL, jiraIssueType = 'STORY'</li>
     *   <li>Nếu myTasks=true: dùng currentUserId, bỏ qua assigneeId param</li>
     *   <li>Progress summary tính trên cùng dataset sau khi apply filter</li>
     * </ul>
     *
     * @param groupId       group ID (bắt buộc)
     * @param requirementId Requirement (Epic) ID — phải thuộc groupId
     * @param statusId      filter theo internal TaskStatus.statusId (optional)
     * @param assigneeId    filter theo internal User.userId (optional; ignored nếu myTasks=true)
     * @param keyword       search trong jiraIssueKey/title (optional)
     * @param myTasks       nếu true dùng currentUserId thay cho assigneeId
     * @param pageable      phân trang + sort
     * @return response bọc epicKey, epicSummary, progressSummary, page of StoryDashboardItemResponse
     */
    StoryListByRequirementResponse getStoriesByRequirement(
            Long groupId,
            Integer requirementId,
            Integer statusId,
            Long assigneeId,
            String keyword,
            boolean myTasks,
            Pageable pageable);

    /**
     * Lấy danh sách Subtask của một Story.
     *
     * <p>Rules:
     * <ul>
     *   <li>storyId phải thuộc groupId AND parentTask IS NULL AND jiraIssueType='STORY' → 404</li>
     *   <li>Không suy ra hợp lệ từ subtask rỗng</li>
     *   <li>Sort: jiraUpdatedAt desc, createdAt desc</li>
     * </ul>
     *
     * @param groupId group ID (bắt buộc)
     * @param storyId PK của Story Task — phải là top-level STORY thuộc groupId
     * @return list SubtaskItemResponse (rỗng nếu không có subtask)
     */
    List<SubtaskItemResponse> getSubtasksByStory(Long groupId, Integer storyId);
}
