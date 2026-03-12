package com.swp391.backend.service;

import com.swp391.backend.dto.response.MyWorkSubtaskDetailResponse;
import com.swp391.backend.dto.response.MyWorkSubtaskListResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service query "My Work – Subtasks" cho màn hình làm việc cá nhân của Team Member.
 * Permission phải được kiểm tra ở controller layer bằng @PreAuthorize.
 */
public interface MyWorkQueryService {

    /**
     * Lấy danh sách Subtask được assign cho current user trong một group.
     *
     * <p>Rules:
     * <ul>
     *   <li>Chỉ Subtask (jiraIssueType = 'SUBTASK') có parentTask hợp lệ</li>
     *   <li>Chỉ assign cho current user (t.assignee.userId = currentUserId)</li>
     *   <li>Nếu current user chưa map jiraAccountId → mappedToJira=false, 200 + page rỗng</li>
     *   <li>Nếu đã map → query bình thường</li>
     *   <li>jiraAccountId chỉ dùng để xác định mappedToJira flag, không làm điều kiện query</li>
     * </ul>
     *
     * @param groupId       group ID (bắt buộc)
     * @param statusId      filter theo internal TaskStatus.statusId (optional)
     * @param priority      filter theo raw priority string, case-insensitive (optional)
     * @param requirementId filter theo Epic/Requirement ID (optional)
     * @param storyId       filter theo parent Story taskId (optional)
     * @param keyword       search trong jiraIssueKey hoặc title (optional)
     * @param pageable      phân trang + sort
     * @return MyWorkSubtaskListResponse với mappedToJira metadata + page of items
     */
    MyWorkSubtaskListResponse getMyWorkSubtasks(
            Long groupId,
            Integer statusId,
            String priority,
            Integer requirementId,
            Integer storyId,
            String keyword,
            Pageable pageable);

    /**
     * Lấy detail một Subtask của current user.
     *
     * <p>Rules:
     * <ul>
     *   <li>taskId phải thuộc groupId + jiraIssueType = SUBTASK + assignee = current user</li>
     *   <li>Nếu không match → BusinessException 404 (leak-safe, không tiết lộ task tồn tại)</li>
     * </ul>
     *
     * @param groupId group ID (bắt buộc)
     * @param taskId  PK của Task
     * @return MyWorkSubtaskDetailResponse với context Story cha + Epic cha
     */
    MyWorkSubtaskDetailResponse getMyWorkSubtaskDetail(Long groupId, Integer taskId);
}
