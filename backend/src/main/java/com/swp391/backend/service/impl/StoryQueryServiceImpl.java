package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.*;
import com.swp391.backend.entity.Requirement;
import com.swp391.backend.entity.Task;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.RequirementRepository;
import com.swp391.backend.repository.TaskRepository;
import com.swp391.backend.security.SecurityService;
import com.swp391.backend.service.StoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements StoryQueryService — bám sát style của RequirementQueryServiceImpl.
 *
 * <p>Rules được enforce:
 * <ul>
 *   <li>requirementId phải thuộc groupId → BusinessException 404</li>
 *   <li>storyId phải là top-level STORY thuộc groupId → BusinessException 404</li>
 *   <li>myTasks=true: override assigneeId bằng currentUserId</li>
 *   <li>Progress summary tính trên cùng dataset sau filter</li>
 *   <li>doneCount theo internal TaskStatus.code = 'DONE'</li>
 *   <li>Không dùng JOIN FETCH trong pageable query</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class StoryQueryServiceImpl implements StoryQueryService {

    private static final String JIRA_ISSUE_TYPE_STORY = "STORY";
    private static final String JIRA_ISSUE_TYPE_SUBTASK = "SUBTASK";
    private static final String STATUS_CODE_DONE = "DONE";

    private final RequirementRepository requirementRepository;
    private final TaskRepository taskRepository;
    private final SecurityService securityService;

    // ─────────────────────────────────────────────────────────────────────────
    // Story list + progress summary
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public StoryListByRequirementResponse getStoriesByRequirement(
            Long groupId,
            Integer requirementId,
            Integer statusId,
            Long assigneeId,
            String keyword,
            boolean myTasks,
            Pageable pageable) {

        // 1. Normalize keyword: trim + blank → null (bám style RequirementQueryServiceImpl)
        String normalizedKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? keyword.trim()
                : null;

        // 2. Resolve effectiveAssigneeId:
        //    - myTasks=true → dùng currentUserId, bỏ qua assigneeId param
        //    - myTasks=false → dùng assigneeId param (có thể null)
        Long effectiveAssigneeId;
        if (myTasks) {
            effectiveAssigneeId = securityService.getCurrentUserId();
        } else {
            effectiveAssigneeId = assigneeId;
        }

        // 3. Validate requirementId thuộc groupId → 404 nếu không match
        Requirement requirement = requirementRepository
                .findByRequirementIdAndStudentGroup_GroupId(requirementId, groupId)
                .orElseThrow(() -> new BusinessException(
                        "Requirement not found or does not belong to group: requirementId="
                                + requirementId + ", groupId=" + groupId,
                        404));

        // 4. Query story page với filters (không dùng JOIN FETCH)
        Page<Task> storyPage = taskRepository.findStoriesByRequirementAndGroup(
                requirementId, groupId, statusId, effectiveAssigneeId, normalizedKeyword, pageable);

        List<Task> stories = storyPage.getContent();

        // 5. Aggregate progress summary trên cùng dataset (cùng filters)
        List<StoryStatusCountProjection> statusCounts = taskRepository
                .countStoriesByStatusForRequirement(
                        requirementId, groupId, statusId, effectiveAssigneeId, normalizedKeyword);

        StoryProgressSummaryResponse progressSummary = buildProgressSummary(statusCounts);

        // 6. Nếu stories rỗng → trả ngay với progress đã tính
        if (stories.isEmpty()) {
            return StoryListByRequirementResponse.builder()
                    .requirementId(requirement.getRequirementId())
                    .epicKey(requirement.getJiraIssueKey())
                    .epicSummary(requirement.getTitle())
                    .progressSummary(progressSummary)
                    .page(new PageImpl<>(Collections.emptyList(), pageable,
                            storyPage.getTotalElements()))
                    .build();
        }

        // 7. Batch-count subtasks để tránh N+1
        List<Integer> storyIds = stories.stream()
                .map(Task::getTaskId)
                .collect(Collectors.toList());

        Map<Integer, Long> subtaskCountMap = toSubtaskCountMap(
                taskRepository.countSubtasksByParentTaskIds(storyIds));

        // 8. Map stories → DTOs
        List<StoryDashboardItemResponse> dtoList = stories.stream()
                .map(t -> toStoryDto(t, subtaskCountMap))
                .collect(Collectors.toList());

        Page<StoryDashboardItemResponse> dtoPage = new PageImpl<>(
                dtoList, pageable, storyPage.getTotalElements());

        return StoryListByRequirementResponse.builder()
                .requirementId(requirement.getRequirementId())
                .epicKey(requirement.getJiraIssueKey())
                .epicSummary(requirement.getTitle())
                .progressSummary(progressSummary)
                .page(dtoPage)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Subtask list
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SubtaskItemResponse> getSubtasksByStory(Long groupId, Integer storyId) {

        // 1. Validate: storyId phải thuộc groupId, là top-level STORY
        //    Không suy ra từ subtask rỗng — explicit fetch + 404
        Task story = taskRepository
                .findByTaskIdAndStudentGroup_GroupIdAndParentTaskIsNullAndJiraIssueType(
                        storyId, groupId, JIRA_ISSUE_TYPE_STORY)
                .orElseThrow(() -> new BusinessException(
                        "Story not found or does not belong to group: storyId="
                                + storyId + ", groupId=" + groupId,
                        404));

        // 2. Query subtasks — sort: jiraUpdatedAt desc, createdAt desc (qua Sort, không dùng NULLS LAST)
        Sort sort = Sort.by(
                Sort.Order.desc("jiraUpdatedAt"),
                Sort.Order.desc("createdAt"));

        List<Task> subtasks = taskRepository.findAllByParentTask_TaskIdAndJiraIssueType(
                story.getTaskId(), JIRA_ISSUE_TYPE_SUBTASK, sort);

        // 3. Map → DTOs (list rỗng là hợp lệ → 200)
        return subtasks.stream()
                .map(t -> toSubtaskDto(t, story.getTaskId()))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Build progress summary từ projection list.
     * doneCount tính theo internal TaskStatus.code = 'DONE'.
     */
    private StoryProgressSummaryResponse buildProgressSummary(
            List<StoryStatusCountProjection> projections) {

        List<StatusCountItem> byStatus = projections.stream()
                .map(p -> StatusCountItem.builder()
                        .statusId(p.getStatusId())
                        .statusCode(p.getStatusCode())
                        .count(p.getCount())
                        .build())
                .collect(Collectors.toList());

        long totalCount = byStatus.stream().mapToLong(StatusCountItem::getCount).sum();

        long doneCount = byStatus.stream()
                .filter(item -> STATUS_CODE_DONE.equalsIgnoreCase(item.getStatusCode()))
                .mapToLong(StatusCountItem::getCount)
                .sum();

        return StoryProgressSummaryResponse.builder()
                .doneCount(doneCount)
                .totalCount(totalCount)
                .byStatus(byStatus)
                .build();
    }

    /** Chuyển SubtaskCountProjection list → Map<parentTaskId, count>. */
    private Map<Integer, Long> toSubtaskCountMap(List<SubtaskCountProjection> projections) {
        return projections.stream()
                .collect(Collectors.toMap(
                        SubtaskCountProjection::getParentTaskId,
                        SubtaskCountProjection::getCount));
    }

    /** Build StoryDashboardItemResponse từ Task entity + subtaskCountMap. */
    private StoryDashboardItemResponse toStoryDto(Task t, Map<Integer, Long> subtaskCountMap) {
        return StoryDashboardItemResponse.builder()
                .taskId(t.getTaskId())
                .requirementId(t.getRequirement() != null
                        ? t.getRequirement().getRequirementId() : null)
                .groupId(t.getStudentGroup() != null
                        ? t.getStudentGroup().getGroupId() : null)
                .storyKey(t.getJiraIssueKey())
                .summary(t.getTitle())
                // status
                .statusId(t.getStatus() != null ? t.getStatus().getStatusId() : null)
                .statusCode(t.getStatus() != null ? t.getStatus().getCode() : null)
                .statusRaw(t.getJiraStatusRaw())
                // priority raw
                .priorityRaw(t.getJiraPriorityRaw())
                // assignee (từ task.assignee — đã được sync qua jira_account_id → user)
                .assigneeId(t.getAssignee() != null ? t.getAssignee().getUserId() : null)
                .assigneeName(t.getAssignee() != null ? t.getAssignee().getFullName() : null)
                // jira updated
                .updated(t.getJiraUpdatedAt())
                // subtask count (batch, không N+1)
                .subtasksCount(subtaskCountMap.getOrDefault(t.getTaskId(), 0L))
                .build();
    }

    /** Build SubtaskItemResponse từ Subtask entity + parentStoryId. */
    private SubtaskItemResponse toSubtaskDto(Task t, Integer parentStoryId) {
        return SubtaskItemResponse.builder()
                .taskId(t.getTaskId())
                .parentStoryId(parentStoryId)
                .subtaskKey(t.getJiraIssueKey())
                .summary(t.getTitle())
                // status
                .statusId(t.getStatus() != null ? t.getStatus().getStatusId() : null)
                .statusCode(t.getStatus() != null ? t.getStatus().getCode() : null)
                .statusRaw(t.getJiraStatusRaw())
                // assignee
                .assigneeId(t.getAssignee() != null ? t.getAssignee().getUserId() : null)
                .assigneeName(t.getAssignee() != null ? t.getAssignee().getFullName() : null)
                // jira updated
                .updated(t.getJiraUpdatedAt())
                .build();
    }
}
