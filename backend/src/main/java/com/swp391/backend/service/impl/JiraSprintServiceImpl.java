package com.swp391.backend.service.impl;

import com.swp391.backend.common.IntegrationTypeIds;
import com.swp391.backend.dto.response.JiraSprintResponse;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.integration.jira.JiraClient;
import com.swp391.backend.integration.jira.dto.JiraBoard;
import com.swp391.backend.integration.jira.dto.JiraBoardListResponse;
import com.swp391.backend.integration.jira.dto.JiraSprint;
import com.swp391.backend.integration.jira.dto.JiraSprintListResponse;
import com.swp391.backend.repository.IntegrationConfigRepository;
import com.swp391.backend.service.JiraSprintService;
import com.swp391.backend.service.TokenCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JiraSprintServiceImpl implements JiraSprintService {

    private final IntegrationConfigRepository integrationConfigRepository;
    private final TokenCryptoService tokenCryptoService;
    private final JiraClient jiraClient;

    private static final String DEFAULT_STATE = "active,future,closed";

    @Override
    @Transactional(readOnly = true)
    public List<JiraSprintResponse> listSprints(Long groupId, String state) {
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
        String effectiveState = (state == null || state.isBlank()) ? DEFAULT_STATE : state;

        // 5. Find boardId for projectKey
        Long boardId = findBoardId(baseUrl, cfg.getJiraEmail(), rawToken, cfg.getProjectKey());

        // 6. Paginate and collect all sprints
        List<JiraSprintResponse> result = new ArrayList<>();
        int startAt = 0;
        int pageSize = 50;

        while (true) {
            JiraSprintListResponse page = jiraClient.getSprintsByBoard(
                    baseUrl, cfg.getJiraEmail(), rawToken, boardId, effectiveState, pageSize, startAt);

            List<JiraSprint> values = page.getValues();
            if (values != null) {
                for (JiraSprint sprint : values) {
                    result.add(mapToResponse(sprint));
                }
            }

            boolean isLast = Boolean.TRUE.equals(page.getIsLast());
            Integer total = page.getTotal();
            if (isLast || values == null || values.isEmpty()
                    || (total != null && startAt + pageSize >= total)) {
                break;
            }
            startAt += pageSize;
        }

        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long findBoardId(String baseUrl, String jiraEmail, String rawToken, String projectKey) {
        JiraBoardListResponse boardList = jiraClient.getBoardsByProject(
                baseUrl, jiraEmail, rawToken, projectKey, 50, 0);

        List<JiraBoard> boards = boardList.getValues();
        if (boards == null || boards.isEmpty()) {
            throw new BusinessException("No Jira board found for projectKey=" + projectKey, 404);
        }
        return boards.get(0).getId();
    }

    private JiraSprintResponse mapToResponse(JiraSprint sprint) {
        return JiraSprintResponse.builder()
                .id(sprint.getId())
                .name(sprint.getName())
                .state(sprint.getState())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .completeDate(sprint.getCompleteDate())
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
