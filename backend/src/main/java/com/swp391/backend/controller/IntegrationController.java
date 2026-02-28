package com.swp391.backend.controller;

import com.swp391.backend.common.IntegrationTypeIds;
import com.swp391.backend.dto.request.GitHubConfigRequest;
import com.swp391.backend.dto.request.JiraConfigRequest;
import com.swp391.backend.dto.response.IntegrationResponse;
import com.swp391.backend.dto.response.JiraIntegrationResponse;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.entity.User;
import com.swp391.backend.mapper.IntegrationMapper;
import com.swp391.backend.repository.IntegrationConfigRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.service.GroupService;
import com.swp391.backend.service.IntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;
    private final IntegrationMapper integrationMapper;
    private final IntegrationConfigRepository repository;
    private final GroupService groupService;
    private final UserRepository userRepository;

    // ── GitHub config endpoints ───────────────────────────────────────────────

    @PostMapping("/{groupId}/github-config")
    public ResponseEntity<IntegrationResponse> saveGitHubConfig(
            @PathVariable Long groupId,
            @Valid @RequestBody GitHubConfigRequest request) {

        checkAuthority(groupId);

        IntegrationConfig config = integrationService.saveOrUpdate(
                groupId,
                request.getRepoFullName(),
                request.getToken());

        return ResponseEntity.ok(integrationMapper.toResponse(config));
    }

    @GetMapping("/{groupId}/github-config")
    public ResponseEntity<IntegrationResponse> getGitHubConfig(@PathVariable Long groupId) {

        checkAuthority(groupId);

        return repository.findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.GITHUB)
                .map(config -> ResponseEntity.ok(integrationMapper.toResponse(config)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Jira config endpoints ─────────────────────────────────────────────────

    @PostMapping("/{groupId}/jira-config")
    public ResponseEntity<JiraIntegrationResponse> saveJiraConfig(
            @PathVariable Long groupId,
            @Valid @RequestBody JiraConfigRequest request) {

        checkAuthority(groupId);

        IntegrationConfig config = integrationService.saveOrUpdateJira(
                groupId,
                request.getBaseUrl(),
                request.getProjectKey(),
                request.getJiraEmail(),
                request.getToken());

        return ResponseEntity.ok(integrationMapper.toJiraResponse(config));
    }

    @GetMapping("/{groupId}/jira-config")
    public ResponseEntity<JiraIntegrationResponse> getJiraConfig(@PathVariable Long groupId) {

        checkAuthority(groupId);

        return repository.findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.JIRA)
                .map(config -> ResponseEntity.ok(integrationMapper.toJiraResponse(config)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Shared authority check ────────────────────────────────────────────────

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
