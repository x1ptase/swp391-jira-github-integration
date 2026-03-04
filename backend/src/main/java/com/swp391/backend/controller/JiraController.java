package com.swp391.backend.controller;

import com.swp391.backend.common.ApiResponse;
import com.swp391.backend.dto.response.JiraIssueExportDto;
import com.swp391.backend.dto.response.JiraIssuePageResponse;
import com.swp391.backend.dto.response.JiraSprintResponse;
import com.swp391.backend.dto.response.JiraVersionResponse;
import com.swp391.backend.entity.User;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.integration.jira.JiraJqlBuilder.FilterType;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.service.GroupService;
import com.swp391.backend.service.JiraIssueService;
import com.swp391.backend.service.JiraLabelService;
import com.swp391.backend.service.JiraSprintService;
import com.swp391.backend.service.JiraVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Jira-related operations (issues export, metadata, etc.).
 * All endpoints require LEADER or ADMIN role within the group.
 */
@RestController
@RequestMapping("/api/integrations/jira")
@RequiredArgsConstructor
public class JiraController {

    private final JiraIssueService jiraIssueService;
    private final JiraSprintService jiraSprintService;
    private final JiraVersionService jiraVersionService;
    private final JiraLabelService jiraLabelService;
    private final GroupService groupService;
    private final UserRepository userRepository;

    // ── Issues ────────────────────────────────────────────────────────────────

    /**
     * Fetch Jira issues for a group's Jira integration.
     *
     * <p>
     * Query params:
     * <ul>
     * <li>filterType – ALL | SPRINT | VERSION | LABEL (default: ALL)</li>
     * <li>sprintId – Long (required when filterType=SPRINT)</li>
     * <li>versionId – String (required when filterType=VERSION)</li>
     * <li>label – String (required when filterType=LABEL)</li>
     * <li>pageToken – String (optional, opaque pagination cursor)</li>
     * <li>maxResults – Integer (default 100, clamped 1..100)</li>
     * <li>fetchAll – Boolean (default true)</li>
     * </ul>
     *
     * <p>
     * Response:
     * <ul>
     * <li>fetchAll=true → {@code ApiResponse<List<JiraIssueExportDto>>}</li>
     * <li>fetchAll=false → {@code ApiResponse<JiraIssuePageResponse>}</li>
     * </ul>
     */
    @GetMapping("/{groupId}/issues")
    public ResponseEntity<ApiResponse<?>> getIssues(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "ALL") String filterType,
            @RequestParam(required = false) Long sprintId,
            @RequestParam(required = false) String versionId,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "100") int maxResults,
            @RequestParam(defaultValue = "true") boolean fetchAll) {

        checkAuthority(groupId);

        FilterType filter;
        try {
            filter = FilterType.valueOf(filterType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Invalid filterType: '" + filterType + "'. Must be ALL, SPRINT, VERSION, or LABEL.", 400);
        }

        if (fetchAll) {
            List<JiraIssueExportDto> issues = jiraIssueService.fetchAllIssues(
                    groupId, filter, sprintId, versionId, label, maxResults);
            return ResponseEntity.ok(ApiResponse.success(issues));
        } else {
            JiraIssuePageResponse page = jiraIssueService.fetchIssuePage(
                    groupId, filter, sprintId, versionId, label, pageToken, maxResults);
            return ResponseEntity.ok(ApiResponse.success(page));
        }
    }

    // ── (1) List Sprints ─────────────────────────────────────────────────────

    /**
     * List Jira sprints for the group's configured project.
     *
     * <p>
     * Query params:
     * <ul>
     * <li>state – "active" | "future" | "closed" hoặc comma-separated
     * (default: "active,future,closed")</li>
     * </ul>
     */
    @GetMapping("/{groupId}/sprints")
    public ResponseEntity<ApiResponse<List<JiraSprintResponse>>> getSprints(
            @PathVariable Long groupId,
            @RequestParam(required = false) String state) {

        checkAuthority(groupId);

        List<JiraSprintResponse> sprints = jiraSprintService.listSprints(groupId, state);
        return ResponseEntity.ok(ApiResponse.success(sprints));
    }

    // ── (2) List Versions ────────────────────────────────────────────────────

    /**
     * List Jira project versions for the group's configured project.
     *
     * <p>
     * Query params:
     * <ul>
     * <li>includeArchived – boolean (default false)</li>
     * <li>includeReleased – boolean (default true)</li>
     * </ul>
     */
    @GetMapping("/{groupId}/versions")
    public ResponseEntity<ApiResponse<List<JiraVersionResponse>>> getVersions(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "true") boolean includeReleased) {

        checkAuthority(groupId);

        List<JiraVersionResponse> versions = jiraVersionService.listVersions(groupId, includeArchived, includeReleased);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    // ── (3) Suggest Labels ───────────────────────────────────────────────────

    /**
     * Suggest Jira labels by aggregating labels from recent issues.
     *
     * <p>
     * Query params:
     * <ul>
     * <li>q – optional prefix/contains filter (case-insensitive)</li>
     * <li>limit – max results (default 30, clamped 1..100)</li>
     * </ul>
     */
    @GetMapping("/{groupId}/labels")
    public ResponseEntity<ApiResponse<List<String>>> getLabels(
            @PathVariable Long groupId,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(defaultValue = "30") int limit) {

        checkAuthority(groupId);

        List<String> labels = jiraLabelService.suggestLabels(groupId, q, limit);
        return ResponseEntity.ok(ApiResponse.success(labels));
    }

    // ── Permission check ──────────────────────────────────────────────────────

    private void checkAuthority(Long groupId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("Unauthorized");
        }

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        boolean isAuthorized = groupService.isUserAuthorized(
                user.getUserId(),
                groupId,
                List.of("LEADER", "ADMIN"));

        if (!isAuthorized) {
            throw new AccessDeniedException("Access denied. Leader or Admin role required for this group.");
        }
    }
}
