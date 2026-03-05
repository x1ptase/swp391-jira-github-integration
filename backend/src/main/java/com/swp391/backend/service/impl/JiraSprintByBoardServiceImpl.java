package com.swp391.backend.service.impl;

import com.swp391.backend.common.IntegrationTypeIds;
import com.swp391.backend.dto.response.JiraSprintDto;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.integration.jira.JiraClient;
import com.swp391.backend.integration.jira.dto.JiraSprint;
import com.swp391.backend.integration.jira.dto.JiraSprintListResponse;
import com.swp391.backend.repository.IntegrationConfigRepository;
import com.swp391.backend.service.JiraSprintByBoardService;
import com.swp391.backend.service.TokenCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JiraSprintByBoardServiceImpl implements JiraSprintByBoardService {

    private final IntegrationConfigRepository integrationConfigRepository;
    private final TokenCryptoService tokenCryptoService;
    private final JiraClient jiraClient;

    private static final String DEFAULT_STATE = "active,future";
    private static final int PAGE_SIZE = 50;
    private static final int MAX_LOOPS = 20;

    @Override
    @Transactional(readOnly = true)
    public List<JiraSprintDto> listSprintsByBoard(Long groupId, Long boardId, String state) {
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

        // 4. Normalize baseUrl + resolve effective state
        String baseUrl = cfg.getBaseUrl().stripTrailing().replaceAll("/+$", "");
        String effectiveState = (state == null || state.isBlank()) ? DEFAULT_STATE : state;

        // 5. Paginate all sprints for the given boardId with safety guard
        List<JiraSprintDto> result = new ArrayList<>();
        int startAt = 0;
        int loops = 0;

        while (loops < MAX_LOOPS) {
            JiraSprintListResponse page = jiraClient.getSprintsByBoard(
                    baseUrl, cfg.getJiraEmail(), rawToken, boardId, effectiveState, PAGE_SIZE, startAt);

            List<JiraSprint> values = page.getValues();
            if (values != null) {
                for (JiraSprint sprint : values) {
                    result.add(JiraSprintDto.builder()
                            .id(sprint.getId())
                            .name(sprint.getName())
                            .build());
                }
            }

            boolean isLast = Boolean.TRUE.equals(page.getIsLast());
            Integer total = page.getTotal();
            if (isLast || values == null || values.isEmpty()
                    || (total != null && startAt + PAGE_SIZE >= total)) {
                break;
            }

            startAt += PAGE_SIZE;
            loops++;
        }

        if (loops >= MAX_LOOPS) {
            throw new BusinessException("Jira pagination aborted (safety guard triggered)", 500);
        }

        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateConfig(IntegrationConfig cfg) {
        if (cfg.getBaseUrl() == null || cfg.getBaseUrl().isBlank()
                || cfg.getProjectKey() == null || cfg.getProjectKey().isBlank()
                || cfg.getJiraEmail() == null || cfg.getJiraEmail().isBlank()
                || cfg.getTokenEncrypted() == null || cfg.getTokenEncrypted().length == 0) {
            throw new BusinessException("Jira integration config is incomplete", 400);
        }
    }
}
