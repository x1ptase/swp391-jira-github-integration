package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.MyWorkSubtaskDetailResponse;
import com.swp391.backend.dto.response.MyWorkSubtaskItemResponse;
import com.swp391.backend.dto.response.MyWorkSubtaskListResponse;
import com.swp391.backend.entity.Requirement;
import com.swp391.backend.entity.Task;
import com.swp391.backend.entity.User;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.TaskRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.security.SecurityService;
import com.swp391.backend.service.MyWorkQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements MyWorkQueryService — bám sát style của StoryQueryServiceImpl.
 *
 * <p>Nguyên tắc query:
 * <ul>
 *   <li>Dataset filter dùng t.assignee.userId = currentUserId (internal mapping)</li>
 *   <li>jiraAccountId chỉ dùng để xác định mappedToJira flag cho FE</li>
 *   <li>Không dùng JOIN FETCH khi pageable list</li>
 *   <li>Dùng LEFT JOIN FETCH khi lấy detail đơn</li>
 *   <li>Null-safe cho parentTask, requirement, assignee, status khi map DTO</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MyWorkQueryServiceImpl implements MyWorkQueryService {

    private static final String JIRA_ISSUE_TYPE_SUBTASK = "SUBTASK";

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final SecurityService securityService;

    // ─────────────────────────────────────────────────────────────────────────
    // List: My Work Subtasks
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public MyWorkSubtaskListResponse getMyWorkSubtasks(
            Long groupId,
            Integer statusId,
            String priority,
            Integer requirementId,
            Integer storyId,
            String keyword,
            Pageable pageable) {

        // 1. Lấy current user ID
        Long currentUserId = securityService.getCurrentUserId();

        // 2. Load current user để kiểm tra jiraAccountId (chỉ dùng cho mappedToJira flag)
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException("Current user not found", 404));

        String jiraAccountId = currentUser.getJiraAccountId();
        boolean mappedToJira = jiraAccountId != null && !jiraAccountId.trim().isEmpty();

        // 3. Nếu chưa map Jira → trả 200 với empty page + mappedToJira=false
        //    FE sẽ biết để hiển thị unmapped state
        if (!mappedToJira) {
            return MyWorkSubtaskListResponse.builder()
                    .mappedToJira(false)
                    .currentUserId(currentUserId)
                    .currentUserJiraAccountId(null)
                    .page(new PageImpl<>(Collections.emptyList(), pageable, 0))
                    .build();
        }

        // 4. Normalize filters: trim + blank → null
        String normalizedKeyword = normalizeString(keyword);
        String normalizedPriority = normalizeString(priority);

        // 5. Query subtasks — điều kiện chính: assignee.userId = currentUserId
        //    (jiraAccountId KHÔNG dùng làm điều kiện query)
        Page<Task> taskPage = taskRepository.findMyWorkSubtasks(
                groupId,
                currentUserId,
                statusId,
                normalizedPriority,
                requirementId,
                storyId,
                normalizedKeyword,
                pageable);

        // 6. Map entities → DTOs (null-safe)
        List<MyWorkSubtaskItemResponse> dtoList = taskPage.getContent().stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());

        Page<MyWorkSubtaskItemResponse> dtoPage = new PageImpl<>(
                dtoList, pageable, taskPage.getTotalElements());

        return MyWorkSubtaskListResponse.builder()
                .mappedToJira(true)
                .currentUserId(currentUserId)
                .currentUserJiraAccountId(jiraAccountId)
                .page(dtoPage)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detail: My Work Subtask detail
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public MyWorkSubtaskDetailResponse getMyWorkSubtaskDetail(Long groupId, Integer taskId) {

        // 1. Lấy current user ID
        Long currentUserId = securityService.getCurrentUserId();

        // 2. Query với LEFT JOIN FETCH — leak-safe: chỉ trả nếu đúng là của current user
        //    Điều kiện: taskId + groupId + assignee.userId = currentUserId + SUBTASK
        Task subtask = taskRepository.findMyWorkSubtaskDetail(taskId, groupId, currentUserId)
                .orElseThrow(() -> new BusinessException(
                        "Subtask not found", 404));

        // 3. Map sang DTO detail với context Story cha + Epic cha (null-safe)
        return toDetailDto(subtask);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapping helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Map Task entity → MyWorkSubtaskItemResponse.
     * Null-safe cho parentTask, parentTask.requirement, status, assignee.
     */
    private MyWorkSubtaskItemResponse toItemDto(Task t) {
        // Parent Story context (null-safe)
        Task parent = t.getParentTask();
        Integer parentStoryId = parent != null ? parent.getTaskId() : null;
        String parentStoryKey = parent != null ? parent.getJiraIssueKey() : null;
        String parentStorySummary = parent != null ? parent.getTitle() : null;

        // Epic context — qua parentTask.requirement (null-safe kép)
        Requirement requirement = parent != null ? parent.getRequirement() : null;
        Integer requirementId = requirement != null ? requirement.getRequirementId() : null;
        String epicKey = requirement != null ? requirement.getJiraIssueKey() : null;
        String epicSummary = requirement != null ? requirement.getTitle() : null;

        return MyWorkSubtaskItemResponse.builder()
                .taskId(t.getTaskId())
                .parentStoryId(parentStoryId)
                .requirementId(requirementId)
                .subtaskKey(t.getJiraIssueKey())
                .summary(t.getTitle())
                // status (null-safe)
                .statusId(t.getStatus() != null ? t.getStatus().getStatusId() : null)
                .statusCode(t.getStatus() != null ? t.getStatus().getCode() : null)
                .statusRaw(t.getJiraStatusRaw())
                // priority
                .priorityRaw(t.getJiraPriorityRaw())
                // parent story context
                .parentStoryKey(parentStoryKey)
                .parentStorySummary(parentStorySummary)
                // epic context
                .epicKey(epicKey)
                .epicSummary(epicSummary)
                // updated
                .updated(t.getJiraUpdatedAt())
                .build();
    }

    /**
     * Map Task entity → MyWorkSubtaskDetailResponse.
     * Null-safe cho parentTask, requirement, assignee, status.
     * (parentTask và assignee, status đã được LEFT JOIN FETCH trong query detail)
     */
    private MyWorkSubtaskDetailResponse toDetailDto(Task t) {
        // Parent Story context (null-safe)
        Task parent = t.getParentTask();
        Integer parentStoryId = parent != null ? parent.getTaskId() : null;
        String parentStoryKey = parent != null ? parent.getJiraIssueKey() : null;
        String parentStorySummary = parent != null ? parent.getTitle() : null;

        // Epic context — qua parentTask.requirement (null-safe kép)
        Requirement requirement = parent != null ? parent.getRequirement() : null;
        Integer requirementId = requirement != null ? requirement.getRequirementId() : null;
        String epicKey = requirement != null ? requirement.getJiraIssueKey() : null;
        String epicSummary = requirement != null ? requirement.getTitle() : null;

        return MyWorkSubtaskDetailResponse.builder()
                .taskId(t.getTaskId())
                .subtaskKey(t.getJiraIssueKey())
                .summary(t.getTitle())
                .description(t.getDescription())
                // status (null-safe)
                .statusId(t.getStatus() != null ? t.getStatus().getStatusId() : null)
                .statusCode(t.getStatus() != null ? t.getStatus().getCode() : null)
                .statusRaw(t.getJiraStatusRaw())
                // priority
                .priorityRaw(t.getJiraPriorityRaw())
                // assignee (null-safe)
                .assigneeId(t.getAssignee() != null ? t.getAssignee().getUserId() : null)
                .assigneeName(t.getAssignee() != null ? t.getAssignee().getFullName() : null)
                // updated
                .updated(t.getJiraUpdatedAt())
                // parent story context
                .parentStoryId(parentStoryId)
                .parentStoryKey(parentStoryKey)
                .parentStorySummary(parentStorySummary)
                // epic context
                .requirementId(requirementId)
                .epicKey(epicKey)
                .epicSummary(epicSummary)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utils
    // ─────────────────────────────────────────────────────────────────────────

    /** Trim string; nếu rỗng sau trim → null. */
    private String normalizeString(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
