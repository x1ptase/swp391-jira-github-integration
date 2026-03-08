package com.swp391.backend.service.impl;

import com.swp391.backend.common.IntegrationTypeIds;
import com.swp391.backend.dto.response.SyncResultResponse;
import com.swp391.backend.entity.*;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.integration.jira.JiraClient;
import com.swp391.backend.integration.jira.dto.*;
import com.swp391.backend.repository.*;
import com.swp391.backend.service.JiraManualSyncService;
import com.swp391.backend.service.SyncLogService;
import com.swp391.backend.service.TokenCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implements 1-way pull sync from Jira → internal DB.
 *
 * <p>
 * Luồng xử lý:
 * 
 * <pre>
 * Phase 1: Fetch all Jira issues (full pagination, safety guards)
 * Phase 2: Upsert Epics → Requirements
 * Phase 3: Upsert Stories → Tasks (parent_task_id = null)
 * Phase 4: Upsert Sub-tasks → Tasks (parent_task_id = Story Task)
 * </pre>
 *
 * <p>
 * Parent linking:
 * <ul>
 * <li>Story parent được lấy từ {@code fields.parent.key} (Jira Cloud)</li>
 * <li>Sub-task parent tương tự từ {@code fields.parent.key}</li>
 * <li>Nếu không resolve được parent → skip + ghi warning vào SyncLog</li>
 * </ul>
 *
 * <p>
 * Assignee: không được map vì user Jira chưa được link với user nội bộ.
 * Giữ null và comment rõ để tránh map sai entity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JiraManualSyncServiceImpl implements JiraManualSyncService {

    // Safety guards để tránh infinite loop khi pagination
    private static final int MAX_LOOPS = 20;
    private static final int MAX_ISSUES = 2000;
    private static final int PAGE_SIZE = 100;

    // Source constant cho SyncLog
    private static final String SOURCE_JIRA = "JIRA";

    // Status codes trong DB (phải khớp data seed)
    private static final String TASK_STATUS_TODO = "TODO";
    private static final String TASK_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String TASK_STATUS_DONE = "DONE";
    private static final String REQ_STATUS_ACTIVE = "ACTIVE";
    private static final String REQ_STATUS_DONE = "DONE";
    private static final String DEFAULT_PRIORITY_CODE = "MEDIUM";

    // Jira issue type names (case-insensitive compare trong code)
    private static final String JIRA_TYPE_EPIC = "epic";
    private static final String JIRA_TYPE_SUBTASK = "subtask";

    private final IntegrationConfigRepository integrationConfigRepository;
    private final TokenCryptoService tokenCryptoService;
    private final JiraClient jiraClient;

    private final RequirementRepository requirementRepository;
    private final TaskRepository taskRepository;
    private final TaskStatusRepository taskStatusRepository;
    private final RequirementStatusRepository requirementStatusRepository;
    private final PriorityRepository priorityRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;
    private final SyncLogService syncLogService;

    // ── Entry point ───────────────────────────────────────────────────────────

    @Override
    public SyncResultResponse syncNow(Long groupId, Long triggeredByUserId) {
        // begin() chạy REQUIRES_NEW → commit ngay, 409 nếu đang RUNNING
        SyncLog syncLog = syncLogService.begin(groupId, SOURCE_JIRA);

        try {
            SyncResultResponse result = doSync(groupId, triggeredByUserId);
            // success() chạy REQUIRES_NEW → commit độc lập
            syncLogService.success(syncLog.getId(), result.getMessage(),
                    result.getInsertedCount(), result.getUpdatedCount());
            return result;
        } catch (Exception e) {
            // fail() chạy REQUIRES_NEW → đảm bảo ghi FAILED dù business tx rollback
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            // Không leak token: chỉ ghi message an toàn
            syncLogService.fail(syncLog.getId(), sanitizeErrorMessage(errMsg));
            throw e; // rethrow để GlobalExceptionHandler xử lý response
        }
    }

    // ── Core sync logic ───────────────────────────────────────────────────────

    /**
     * Thực thi toàn bộ sync trong transaction riêng cho business data.
     */
    @Transactional
    protected SyncResultResponse doSync(Long groupId, Long triggeredByUserId) {
        // 1. Load config
        IntegrationConfig config = loadAndValidateConfig(groupId);
        String rawToken = decryptToken(config);
        String baseUrl = normalizeBaseUrl(config.getBaseUrl());

        // 2. Load context entities (user, group)
        User triggeredBy = userRepository.findById(triggeredByUserId)
                .orElseThrow(() -> new BusinessException(
                        "User not found with id: " + triggeredByUserId, 404));
        StudentGroup studentGroup = studentGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(
                        "Group not found with id: " + groupId, 404));

        // 3. Load lookup tables vào bộ nhớ để tránh N+1
        Map<String, TaskStatus> taskStatusMap = loadTaskStatusMap();
        Map<String, RequirementStatus> reqStatusMap = loadRequirementStatusMap();
        Priority defaultPriority = loadDefaultPriority();

        // 4. Phase 1: Fetch toàn bộ issues từ Jira
        String jql = "project = \"" + config.getProjectKey() + "\" ORDER BY created ASC";
        List<JiraIssue> allIssues = fetchAllIssues(baseUrl, config.getJiraEmail(), rawToken, jql);

        if (allIssues.isEmpty()) {
            return SyncResultResponse.builder()
                    .status("SUCCESS")
                    .insertedCount(0)
                    .updatedCount(0)
                    .message("Jira sync completed. No issues found.")
                    .build();
        }

        // 5. Phân loại issues
        List<JiraIssue> epics = new ArrayList<>();
        List<JiraIssue> stories = new ArrayList<>();
        List<JiraIssue> subtasks = new ArrayList<>();
        classifyIssues(allIssues, epics, stories, subtasks);

        int inserted = 0;
        int updated = 0;
        List<String> warnings = new ArrayList<>();

        // 6. Phase 2: Upsert Epics → Requirements
        // Preload existing requirements theo jira_issue_key để batch upsert
        List<String> epicKeys = epics.stream().map(JiraIssue::getKey).toList();
        Map<String, Requirement> existingReqs = requirementRepository
                .findAllByJiraIssueKeyIn(epicKeys).stream()
                .collect(Collectors.toMap(Requirement::getJiraIssueKey, Function.identity()));

        List<Requirement> reqsToSave = new ArrayList<>();
        // Map: epicKey → Requirement (dùng sau để link Story)
        Map<String, Requirement> epicKeyToReq = new HashMap<>();

        for (JiraIssue epic : epics) {
            JiraFields f = epic.getFields();
            if (f == null)
                continue;

            boolean isNew = !existingReqs.containsKey(epic.getKey());
            Requirement req = isNew ? new Requirement() : existingReqs.get(epic.getKey());

            req.setJiraIssueKey(epic.getKey());
            req.setTitle(truncate(f.getSummary(), 200));
            req.setDescription(extractPlainText(f.getDescription()));
            req.setStudentGroup(studentGroup);
            req.setPriority(defaultPriority); // Jira priority không map 1-1 với internal, dùng default
            req.setStatus(mapRequirementStatus(f.getStatus(), reqStatusMap));

            // createdBy: luôn set bằng user trigger sync để tránh null
            if (isNew || req.getCreatedBy() == null) {
                req.setCreatedBy(triggeredBy);
            }

            // --- Jira metadata mới ---
            req.setJiraIssueType("EPIC");
            req.setJiraStatusRaw(extractRawStatusName(f));
            req.setJiraPriorityRaw(extractRawPriorityName(f));
            req.setJiraUpdatedAt(parseJiraUpdatedAt(f.getUpdated()));

            reqsToSave.add(req);
            epicKeyToReq.put(epic.getKey(), req);

            if (isNew)
                inserted++;
            else
                updated++;
        }

        // Phase 2 flush: save Requirements trước để chúng có ID
        List<Requirement> savedReqs = requirementRepository.saveAll(reqsToSave);
        // Cập nhật map với entity đã có ID (sau khi saveAll)
        for (Requirement saved : savedReqs) {
            epicKeyToReq.put(saved.getJiraIssueKey(), saved);
        }

        // 7. Phase 3: Upsert Stories → Tasks
        List<String> storyKeys = stories.stream().map(JiraIssue::getKey).toList();
        Map<String, Task> existingStoryTasks = taskRepository
                .findAllByJiraIssueKeyIn(storyKeys).stream()
                .collect(Collectors.toMap(Task::getJiraIssueKey, Function.identity()));

        List<Task> storyTasksToSave = new ArrayList<>();
        // Map: storyKey → Task (dùng sau để link Sub-task)
        Map<String, Task> storyKeyToTask = new HashMap<>();

        for (JiraIssue story : stories) {
            JiraFields f = story.getFields();
            if (f == null) {
                warnings.add("Story " + story.getKey() + " has no fields, skipped.");
                continue;
            }

            // Resolve parent Epic
            String epicKey = resolveParentKey(f);
            if (epicKey == null || !epicKeyToReq.containsKey(epicKey)) {
                // Fallback: thử từ existing DB nếu Epic đã sync trước đó
                Requirement fallbackReq = epicKey != null
                        ? requirementRepository.findByJiraIssueKey(epicKey).orElse(null)
                        : null;
                if (fallbackReq == null) {
                    warnings.add("Story " + story.getKey() + " skipped: parent Epic '"
                            + epicKey + "' not found in current sync or DB.");
                    continue;
                }
                epicKeyToReq.put(epicKey, fallbackReq);
            }

            Requirement parentReq = epicKeyToReq.get(epicKey);
            boolean isNew = !existingStoryTasks.containsKey(story.getKey());
            Task task = isNew ? new Task() : existingStoryTasks.get(story.getKey());

            task.setJiraIssueKey(story.getKey());
            task.setTitle(truncate(f.getSummary(), 200));
            task.setDescription(extractPlainText(f.getDescription()));
            task.setRequirement(parentReq);
            task.setStudentGroup(studentGroup);
            task.setStatus(mapTaskStatus(f.getStatus(), taskStatusMap));
            task.setParentTask(null); // Story là top-level task, không có parent

            // --- Jira metadata mới ---
            task.setJiraIssueType("STORY");
            task.setJiraParentIssueKey(epicKey);
            task.setJiraStatusRaw(extractRawStatusName(f));
            task.setJiraPriorityRaw(extractRawPriorityName(f));
            task.setJiraUpdatedAt(parseJiraUpdatedAt(f.getUpdated()));

            // Assignee resolution qua jira_account_id (không dùng displayName)
            String storyAssigneeAccountId = extractAssigneeAccountId(f);
            task.setJiraAssigneeAccountId(storyAssigneeAccountId);
            task.setAssignee(resolveAssigneeByJiraAccountId(storyAssigneeAccountId).orElse(null));

            storyTasksToSave.add(task);
            storyKeyToTask.put(story.getKey(), task);

            if (isNew)
                inserted++;
            else
                updated++;
        }

        // Phase 3 flush: save Story Tasks trước để chúng có ID
        List<Task> savedStoryTasks = taskRepository.saveAll(storyTasksToSave);
        for (Task saved : savedStoryTasks) {
            storyKeyToTask.put(saved.getJiraIssueKey(), saved);
        }

        // 8. Phase 4: Upsert Sub-tasks → child Tasks
        List<String> subtaskKeys = subtasks.stream().map(JiraIssue::getKey).toList();
        Map<String, Task> existingSubtasks = taskRepository
                .findAllByJiraIssueKeyIn(subtaskKeys).stream()
                .collect(Collectors.toMap(Task::getJiraIssueKey, Function.identity()));

        List<Task> subtaskTasksToSave = new ArrayList<>();

        for (JiraIssue subtask : subtasks) {
            JiraFields f = subtask.getFields();
            if (f == null) {
                warnings.add("Sub-task " + subtask.getKey() + " has no fields, skipped.");
                continue;
            }

            // Resolve parent Story
            String storyKey = resolveParentKey(f);
            if (storyKey == null || !storyKeyToTask.containsKey(storyKey)) {
                // Fallback: thử từ DB nếu Story đã sync trước đó
                Task fallbackStoryTask = storyKey != null
                        ? taskRepository.findByJiraIssueKey(storyKey).orElse(null)
                        : null;
                if (fallbackStoryTask == null) {
                    warnings.add("Sub-task " + subtask.getKey() + " skipped: parent Story '"
                            + storyKey + "' not found in current sync or DB.");
                    continue;
                }
                storyKeyToTask.put(storyKey, fallbackStoryTask);
            }

            Task parentStoryTask = storyKeyToTask.get(storyKey);
            // Requirement của sub-task = requirement của Story cha
            Requirement parentReq = parentStoryTask.getRequirement();

            boolean isNew = !existingSubtasks.containsKey(subtask.getKey());
            Task task = isNew ? new Task() : existingSubtasks.get(subtask.getKey());

            task.setJiraIssueKey(subtask.getKey());
            task.setTitle(truncate(f.getSummary(), 200));
            task.setDescription(extractPlainText(f.getDescription()));
            task.setRequirement(parentReq);
            task.setStudentGroup(studentGroup);
            task.setStatus(mapTaskStatus(f.getStatus(), taskStatusMap));
            task.setParentTask(parentStoryTask); // link đúng FK

            // --- Jira metadata mới ---
            task.setJiraIssueType("SUBTASK");
            task.setJiraParentIssueKey(storyKey);
            task.setJiraStatusRaw(extractRawStatusName(f));
            task.setJiraPriorityRaw(extractRawPriorityName(f));
            task.setJiraUpdatedAt(parseJiraUpdatedAt(f.getUpdated()));

            // Assignee resolution qua jira_account_id (không dùng displayName)
            String subtaskAssigneeAccountId = extractAssigneeAccountId(f);
            task.setJiraAssigneeAccountId(subtaskAssigneeAccountId);
            task.setAssignee(resolveAssigneeByJiraAccountId(subtaskAssigneeAccountId).orElse(null));

            subtaskTasksToSave.add(task);

            if (isNew)
                inserted++;
            else
                updated++;
        }

        // Phase 4 flush: save Sub-tasks
        taskRepository.saveAll(subtaskTasksToSave);

        // 9. Build final message kèm warnings nếu có
        StringBuilder msg = new StringBuilder("Jira sync completed.");
        if (!warnings.isEmpty()) {
            msg.append(" Warnings (").append(warnings.size()).append("): ")
                    .append(String.join("; ", warnings));
        }

        return SyncResultResponse.builder()
                .status("SUCCESS")
                .insertedCount(inserted)
                .updatedCount(updated)
                .message(msg.toString())
                .build();
    }

    // ── Jira fetch (full pagination) ──────────────────────────────────────────

    /**
     * Fetch tất cả issues theo JQL với full pagination.
     * Dùng searchIssueIdsByJql + bulkFetchIssueDetails (giống
     * JiraIssueServiceImpl).
     * Safety guards: MAX_LOOPS, MAX_ISSUES, token-not-advancing.
     */
    private List<JiraIssue> fetchAllIssues(String baseUrl, String jiraEmail,
            String token, String jql) {
        List<JiraIssue> collected = new ArrayList<>();
        String currentToken = null;
        String prevToken = null;
        int loops = 0;
        boolean firstLoop = true;

        while (true) {
            if (loops >= MAX_LOOPS) {
                throw new BusinessException(
                        "Jira sync pagination aborted: max loops (" + MAX_LOOPS + ") reached", 500);
            }
            if (collected.size() >= MAX_ISSUES) {
                throw new BusinessException(
                        "Jira sync pagination aborted: max issues (" + MAX_ISSUES + ") reached", 500);
            }
            // Guard: token không thoát (chỉ check sau loop đầu)
            if (!firstLoop && Objects.equals(currentToken, prevToken)) {
                throw new BusinessException(
                        "Jira sync pagination aborted: nextPageToken not advancing", 500);
            }

            // Bước 1: Lấy danh sách ID theo JQL
            JiraSearchJqlResponse searchResp = jiraClient.searchIssueIdsByJql(
                    baseUrl, jiraEmail, token, jql, PAGE_SIZE, currentToken);

            List<String> ids = extractIds(searchResp.getIssues());
            if (ids.isEmpty())
                break;

            // Bước 2: Bulk-fetch chi tiết (bao gồm parent, status, issuetype)
            JiraBulkFetchResponse bulkResp = jiraClient.bulkFetchIssueDetails(
                    baseUrl, jiraEmail, token, ids);

            if (bulkResp.getIssues() != null) {
                collected.addAll(bulkResp.getIssues());
            }

            boolean isLast = Boolean.TRUE.equals(searchResp.getIsLast())
                    || searchResp.getNextPageToken() == null;
            if (isLast)
                break;

            prevToken = currentToken;
            currentToken = searchResp.getNextPageToken();
            firstLoop = false;
            loops++;
        }

        return collected;
    }

    private List<String> extractIds(List<JiraIssueIdRef> refs) {
        if (refs == null || refs.isEmpty())
            return List.of();
        return refs.stream()
                .filter(r -> r.getId() != null)
                .map(JiraIssueIdRef::getId)
                .toList();
    }

    // ── Classification ────────────────────────────────────────────────────────

    /**
     * Phân loại issues thành Epic / Story / Sub-task.
     * Type được xác định bằng fields.issuetype.name (case-insensitive).
     * Issues không có fields hoặc issuetype bị bỏ qua.
     */
    private void classifyIssues(List<JiraIssue> issues,
            List<JiraIssue> epics, List<JiraIssue> stories, List<JiraIssue> subtasks) {
        for (JiraIssue issue : issues) {
            if (issue.getFields() == null || issue.getFields().getIssuetype() == null)
                continue;

            String typeName = issue.getFields().getIssuetype().getName();
            if (typeName == null)
                continue;

            String typeNorm = typeName.trim().toLowerCase();

            if (typeNorm.equals(JIRA_TYPE_EPIC)) {
                epics.add(issue);
            } else if (typeNorm.equals(JIRA_TYPE_SUBTASK) || typeNorm.equals("sub-task")) {
                subtasks.add(issue);
            } else {
                // Story, Task, Bug, Feature, ... đều map thành Story-level Task
                stories.add(issue);
            }
        }
    }

    // ── Parent resolution ─────────────────────────────────────────────────────

    /**
     * Lấy key của parent issue từ fields.parent.key.
     * Trả null nếu không có parent (hoặc parent.key vắng mặt).
     *
     * <p>
     * Jira Cloud: cả Story và Sub-task đều có parent field:
     * <ul>
     * <li>Story.parent.key = Epic key</li>
     * <li>Sub-task.parent.key = Story key</li>
     * </ul>
     */
    private String resolveParentKey(JiraFields fields) {
        if (fields == null)
            return null;
        JiraParentRef parent = fields.getParent();
        if (parent == null)
            return null;
        String key = parent.getKey();
        return (key != null && !key.isBlank()) ? key.trim() : null;
    }

    // ── Status mapping ────────────────────────────────────────────────────────

    /**
     * Map Jira status → TaskStatus theo statusCategory.key.
     * Fallback an toàn về TODO nếu statusCategory null hoặc key không nhận ra.
     */
    private TaskStatus mapTaskStatus(JiraName jiraStatus, Map<String, TaskStatus> statusMap) {
        String categoryKey = extractStatusCategoryKey(jiraStatus);
        String code = switch (categoryKey) {
            case "done" -> TASK_STATUS_DONE;
            case "indeterminate" -> TASK_STATUS_IN_PROGRESS;
            default -> TASK_STATUS_TODO; // "new" hoặc unknown → TODO
        };
        TaskStatus result = statusMap.get(code);
        if (result == null) {
            // Fallback cứng nếu DB chưa có code
            result = statusMap.values().stream().findFirst()
                    .orElseThrow(() -> new BusinessException("No TaskStatus found in DB", 500));
        }
        return result;
    }

    /**
     * Map Jira status → RequirementStatus.
     * "done" → DONE, còn lại → ACTIVE.
     */
    private RequirementStatus mapRequirementStatus(JiraName jiraStatus,
            Map<String, RequirementStatus> statusMap) {
        String categoryKey = extractStatusCategoryKey(jiraStatus);
        String code = "done".equals(categoryKey) ? REQ_STATUS_DONE : REQ_STATUS_ACTIVE;
        RequirementStatus result = statusMap.get(code);
        if (result == null) {
            result = statusMap.values().stream().findFirst()
                    .orElseThrow(() -> new BusinessException("No RequirementStatus found in DB", 500));
        }
        return result;
    }

    /**
     * Lấy statusCategory.key một cách an toàn.
     * Trả "new" nếu bất kỳ field nào null (fallback → TODO).
     */
    private String extractStatusCategoryKey(JiraName jiraStatus) {
        if (jiraStatus == null)
            return "new";
        JiraStatusCategory cat = jiraStatus.getStatusCategory();
        if (cat == null)
            return "new";
        String key = cat.getKey();
        return (key != null) ? key.trim().toLowerCase() : "new";
    }

    // ── Lookup loaders ────────────────────────────────────────────────────────

    private Map<String, TaskStatus> loadTaskStatusMap() {
        return taskStatusRepository.findAll().stream()
                .collect(Collectors.toMap(TaskStatus::getCode, Function.identity()));
    }

    private Map<String, RequirementStatus> loadRequirementStatusMap() {
        return requirementStatusRepository.findAll().stream()
                .collect(Collectors.toMap(RequirementStatus::getCode, Function.identity()));
    }

    private Priority loadDefaultPriority() {
        return priorityRepository.findByCode(DEFAULT_PRIORITY_CODE)
                .or(() -> priorityRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new BusinessException("No Priority found in DB", 500));
    }

    // ── Config helpers ────────────────────────────────────────────────────────

    private IntegrationConfig loadAndValidateConfig(Long groupId) {
        IntegrationConfig config = integrationConfigRepository
                .findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.JIRA)
                .orElseThrow(() -> new BusinessException(
                        "Jira integration configuration not found for group: " + groupId, 404));

        if (config.getTokenEncrypted() == null) {
            throw new BusinessException("Jira token is missing in configuration", 400);
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            throw new BusinessException("Jira baseUrl is missing in configuration", 400);
        }
        if (config.getProjectKey() == null || config.getProjectKey().isBlank()) {
            throw new BusinessException("Jira projectKey is missing in configuration", 400);
        }
        if (config.getJiraEmail() == null || config.getJiraEmail().isBlank()) {
            throw new BusinessException("Jira email is missing in configuration", 400);
        }
        return config;
    }

    private String decryptToken(IntegrationConfig config) {
        try {
            return tokenCryptoService.decryptFromBytes(config.getTokenEncrypted());
        } catch (Exception e) {
            throw new BusinessException("Failed to decrypt Jira token", 500);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.stripTrailing().replaceAll("/+$", "");
    }

    // ── ADF → Plain text ──────────────────────────────────────────────────────

    /**
     * Converts Atlassian Document Format (ADF) to plain text.
     * Xử lý an toàn: null, String thuần, Map (ADF), List.
     */
    @SuppressWarnings("unchecked")
    private String extractPlainText(Object description) {
        if (description == null)
            return null;
        if (description instanceof String s)
            return s.isBlank() ? null : s;
        if (description instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder();
            traverseAdf((Map<String, Object>) map, sb);
            String result = sb.toString().trim();
            return result.isEmpty() ? null : result;
        }
        if (description instanceof List<?> list) {
            // Hiếm gặp nhưng handle để không crash
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    traverseAdf((Map<String, Object>) map, sb);
                }
            }
            String result = sb.toString().trim();
            return result.isEmpty() ? null : result;
        }
        return description.toString();
    }

    @SuppressWarnings("unchecked")
    private void traverseAdf(Map<String, Object> node, StringBuilder sb) {
        Object type = node.get("type");
        Object text = node.get("text");

        if ("text".equals(type) && text instanceof String textStr) {
            sb.append(textStr);
        }

        Object content = node.get("content");
        if (content instanceof List<?> contentList) {
            for (Object child : contentList) {
                if (child instanceof Map<?, ?> childMap) {
                    traverseAdf((Map<String, Object>) childMap, sb);
                }
            }
            if (isBlockNode(type))
                sb.append("\n");
        }
    }

    private boolean isBlockNode(Object type) {
        if (!(type instanceof String t))
            return false;
        return switch (t) {
            case "paragraph", "heading", "bulletList", "orderedList",
                    "listItem", "blockquote", "codeBlock" ->
                true;
            default -> false;
        };
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private String truncate(String s, int maxLen) {
        if (s == null)
            return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    /**
     * Sanitize error message để không leak token hoặc credentials.
     */
    private String sanitizeErrorMessage(String msg) {
        if (msg == null)
            return "Unknown error";
        // Cắt dài quá 500 chars
        String cleaned = msg.length() > 500 ? msg.substring(0, 500) + "..." : msg;
        // Không dùng regex để tránh false positive, chỉ cắt ngắn
        return cleaned;
    }

    // ── Jira metadata helpers ─────────────────────────────────────────────────

    /**
     * Trả về normalized issue type name (UPPER_CASE) hoặc null.
     * Dùng để fill jira_issue_type; gọi getter trực tiếp an toàn hơn.
     */
    private String extractIssueTypeName(JiraFields fields) {
        if (fields == null || fields.getIssuetype() == null)
            return null;
        String name = fields.getIssuetype().getName();
        return (name != null && !name.isBlank()) ? name.trim().toUpperCase() : null;
    }

    /**
     * Trả về raw status name từ Jira. Null-safe.
     */
    private String extractRawStatusName(JiraFields fields) {
        if (fields == null || fields.getStatus() == null)
            return null;
        String name = fields.getStatus().getName();
        return (name != null && !name.isBlank()) ? name.trim() : null;
    }

    /**
     * Trả về raw priority name từ Jira. Null-safe.
     * priority là optional trong Jira, nên có thể null.
     */
    private String extractRawPriorityName(JiraFields fields) {
        if (fields == null || fields.getPriority() == null)
            return null;
        String name = fields.getPriority().getName();
        return (name != null && !name.isBlank()) ? name.trim() : null;
    }

    /**
     * Trả về assignee accountId từ Jira. Null-safe.
     */
    private String extractAssigneeAccountId(JiraFields fields) {
        if (fields == null || fields.getAssignee() == null)
            return null;
        String accountId = fields.getAssignee().getAccountId();
        return (accountId != null && !accountId.isBlank()) ? accountId.trim() : null;
    }

    /**
     * Parse chuỗi ISO datetime của Jira (có offset timezone) sang LocalDateTime.
     * Ví dụ: "2024-01-15T10:30:00.000+0700" hoặc "2024-01-15T10:30:00.000+07:00".
     * Trả null nếu input null/blank/parse lỗi.
     */
    private LocalDateTime parseJiraUpdatedAt(String updated) {
        if (updated == null || updated.isBlank())
            return null;
        try {
            return OffsetDateTime.parse(updated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .toLocalDateTime();
        } catch (Exception e1) {
            // Thử pattern có milli không có colon trong timezone offset: +0700
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxx");
                return OffsetDateTime.parse(updated, fmt).toLocalDateTime();
            } catch (Exception e2) {
                log.warn("Cannot parse Jira updated datetime '{}': {}", updated, e2.getMessage());
                return null;
            }
        }
    }

    /**
     * Tìm User trong DB theo jira_account_id.
     * Trả Optional.empty() nếu accountId null/blank hoặc không tìm thấy.
     * Không throw exception – caller xử lý null gracefully.
     */
    private Optional<User> resolveAssigneeByJiraAccountId(String jiraAccountId) {
        if (jiraAccountId == null || jiraAccountId.isBlank())
            return Optional.empty();
        return userRepository.findByJiraAccountId(jiraAccountId);
    }
}
