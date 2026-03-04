package com.swp391.backend.service.impl;

import com.swp391.backend.common.IntegrationTypeIds;
import com.swp391.backend.dto.response.JiraVersionResponse;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.integration.jira.JiraClient;
import com.swp391.backend.integration.jira.dto.JiraVersion;
import com.swp391.backend.repository.IntegrationConfigRepository;
import com.swp391.backend.service.JiraVersionService;
import com.swp391.backend.service.TokenCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JiraVersionServiceImpl implements JiraVersionService {

    private final IntegrationConfigRepository integrationConfigRepository;
    private final TokenCryptoService tokenCryptoService;
    private final JiraClient jiraClient;

    @Override
    @Transactional(readOnly = true)
    public List<JiraVersionResponse> listVersions(Long groupId, boolean includeArchived, boolean includeReleased) {
        // 1. Load config
        IntegrationConfig cfg = integrationConfigRepository
                .findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.JIRA)
                .orElseThrow(() -> new BusinessException(
                        "Jira integration configuration not found for group: " + groupId, 404));

        // 2. Validate config
        validateConfig(cfg);

        // 3. Decrypt token
        String rawToken;
        try {
            rawToken = tokenCryptoService.decryptFromBytes(cfg.getTokenEncrypted());
        } catch (Exception e) {
            throw new BusinessException("Failed to decrypt Jira token", 500);
        }

        // 4. Normalize baseUrl
        String baseUrl = cfg.getBaseUrl().stripTrailing().replaceAll("/+$", "");

        // 5. Call Jira API
        List<JiraVersion> versions = jiraClient.getProjectVersions(
                baseUrl, cfg.getJiraEmail(), rawToken, cfg.getProjectKey());

        // 6. Filter and map
        return versions.stream()
                .filter(v -> includeArchived || !Boolean.TRUE.equals(v.getArchived()))
                .filter(v -> includeReleased || !Boolean.TRUE.equals(v.getReleased()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JiraVersionResponse mapToResponse(JiraVersion v) {
        return JiraVersionResponse.builder()
                .id(v.getId())
                .name(v.getName())
                .released(v.getReleased())
                .archived(v.getArchived())
                .releaseDate(v.getReleaseDate())
                .description(v.getDescription())
                .build();
    }

    private void validateConfig(IntegrationConfig cfg) {
        if (cfg.getBaseUrl() == null || cfg.getBaseUrl().isBlank()
                || cfg.getProjectKey() == null || cfg.getProjectKey().isBlank()
                || cfg.getJiraEmail() == null || cfg.getJiraEmail().isBlank()
                || cfg.getTokenEncrypted() == null || cfg.getTokenEncrypted().length == 0) {
            throw new BusinessException("Jira integration config is incomplete", 400);
        }
    }
}
