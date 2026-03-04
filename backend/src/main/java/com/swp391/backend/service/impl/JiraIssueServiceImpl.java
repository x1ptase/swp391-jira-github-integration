package com.swp391.backend.service.impl;

import com.swp391.backend.common.IntegrationTypeIds;
import com.swp391.backend.dto.response.JiraIssueExportDto;
import com.swp391.backend.dto.response.JiraIssuePageResponse;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.integration.jira.JiraClient;
import com.swp391.backend.integration.jira.JiraJqlBuilder;
import com.swp391.backend.integration.jira.JiraJqlBuilder.FilterType;
import com.swp391.backend.integration.jira.dto.JiraAssignee;
import com.swp391.backend.integration.jira.dto.JiraBulkFetchResponse;
import com.swp391.backend.integration.jira.dto.JiraFields;
import com.swp391.backend.integration.jira.dto.JiraIssue;
import com.swp391.backend.integration.jira.dto.JiraIssueIdRef;
import com.swp391.backend.integration.jira.dto.JiraName;
import com.swp391.backend.integration.jira.dto.JiraSearchJqlResponse;
import com.swp391.backend.repository.IntegrationConfigRepository;
import com.swp391.backend.service.JiraIssueService;
import com.swp391.backend.service.TokenCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class JiraIssueServiceImpl implements JiraIssueService {

    // Safety guards for fetch-all loop
    private static final int MAX_LOOPS = 20;
    private static final int MAX_ISSUES = 2000;

    private final IntegrationConfigRepository integrationConfigRepository;
    private final TokenCryptoService tokenCryptoService;
    private final JiraClient jiraClient;
    private final JiraJqlBuilder jiraJqlBuilder;

    // ── fetchAllIssues ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<JiraIssueExportDto> fetchAllIssues(Long groupId, FilterType filterType,
            Long sprintId, String versionId, String label,
            int maxResults) {
        JiraContext ctx = buildContext(groupId, filterType, sprintId, versionId, label, maxResults);
        return loopAllPages(ctx);
    }

    // ── fetchIssuePage ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public JiraIssuePageResponse fetchIssuePage(Long groupId, FilterType filterType,
            Long sprintId, String versionId, String label,
            String pageToken, int maxResults) {
        JiraContext ctx = buildContext(groupId, filterType, sprintId, versionId, label, maxResults);

        JiraSearchJqlResponse searchResp = jiraClient.searchIssueIdsByJql(
                ctx.baseUrl(), ctx.jiraEmail(), ctx.token(),
                ctx.jql(), ctx.effectiveMax(), pageToken);

        List<String> ids = extractIds(searchResp.getIssues());
        List<JiraIssueExportDto> items = ids.isEmpty() ? List.of() : fetchAndMap(ctx, ids);

        return JiraIssuePageResponse.builder()
                .items(items)
                .nextPageToken(searchResp.getNextPageToken())
                .isLast(Boolean.TRUE.equals(searchResp.getIsLast())
                        || searchResp.getNextPageToken() == null)
                .build();
    }

    // ── Core fetch-all loop ───────────────────────────────────────────────────

    private List<JiraIssueExportDto> loopAllPages(JiraContext ctx) {
        List<JiraIssueExportDto> collected = new ArrayList<>();
        String currentToken = null;
        String prevToken = null;
        int loops = 0;
        boolean firstLoop = true;

        while (true) {
            // Safety guard: max loops
            if (loops >= MAX_LOOPS) {
                throw new BusinessException(
                        "Jira pagination aborted (safety guard triggered): max loops reached", 500);
            }
            // Safety guard: max issues
            if (collected.size() >= MAX_ISSUES) {
                throw new BusinessException(
                        "Jira pagination aborted (safety guard triggered): max issues limit (" + MAX_ISSUES
                                + ") reached",
                        500);
            }
            // Safety guard: token not advancing (only checked after first round)
            if (!firstLoop && Objects.equals(currentToken, prevToken)) {
                throw new BusinessException(
                        "Jira pagination aborted (safety guard triggered): nextPageToken not advancing", 500);
            }

            JiraSearchJqlResponse searchResp = jiraClient.searchIssueIdsByJql(
                    ctx.baseUrl(), ctx.jiraEmail(), ctx.token(),
                    ctx.jql(), ctx.effectiveMax(), currentToken);

            List<String> ids = extractIds(searchResp.getIssues());

            // Safety guard: empty ids
            if (ids.isEmpty())
                break;

            collected.addAll(fetchAndMap(ctx, ids));

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JiraContext buildContext(Long groupId, FilterType filterType,
            Long sprintId, String versionId, String label,
            int maxResults) {
        // 1. Load config
        IntegrationConfig config = integrationConfigRepository
                .findByGroupIdAndIntegrationTypeId(groupId, IntegrationTypeIds.JIRA)
                .orElseThrow(() -> new BusinessException(
                        "Jira integration configuration not found for group: " + groupId, 404));

        validateConfig(config);

        // 2. Decrypt token – never log
        String rawToken;
        try {
            rawToken = tokenCryptoService.decryptFromBytes(config.getTokenEncrypted());
        } catch (Exception e) {
            throw new BusinessException("Failed to decrypt Jira token", 500);
        }

        // 3. Build JQL (validation + quoting inside JiraJqlBuilder)
        String jql = jiraJqlBuilder.buildJql(
                config.getProjectKey(), filterType, sprintId, versionId, label);

        // 4. Clamp maxResults
        int effectiveMax = Math.max(1, Math.min(maxResults, 100));

        return new JiraContext(config.getBaseUrl(), config.getJiraEmail(), rawToken, jql, effectiveMax);
    }

    private List<String> extractIds(List<JiraIssueIdRef> refs) {
        if (refs == null || refs.isEmpty())
            return List.of();
        return refs.stream()
                .filter(r -> r.getId() != null)
                .map(JiraIssueIdRef::getId)
                .toList();
    }

    private List<JiraIssueExportDto> fetchAndMap(JiraContext ctx, List<String> ids) {
        JiraBulkFetchResponse bulkResp = jiraClient.bulkFetchIssueDetails(
                ctx.baseUrl(), ctx.jiraEmail(), ctx.token(), ids);
        return mapToDto(bulkResp.getIssues());
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private List<JiraIssueExportDto> mapToDto(List<JiraIssue> issues) {
        if (issues == null)
            return List.of();
        return issues.stream()
                .map(this::toDto)
                .toList();
    }

    private JiraIssueExportDto toDto(JiraIssue issue) {
        JiraFields f = issue.getFields();
        if (f == null) {
            return JiraIssueExportDto.builder()
                    .key(issue.getKey())
                    .build();
        }

        return JiraIssueExportDto.builder()
                .key(issue.getKey())
                .summary(f.getSummary())
                .description(extractPlainText(f.getDescription()))
                .issueType(nameOf(f.getIssuetype()))
                .status(nameOf(f.getStatus()))
                .priority(nameOf(f.getPriority()))
                .assignee(displayNameOf(f.getAssignee()))
                .updated(f.getUpdated())
                .build();
    }

    private String nameOf(JiraName jiraName) {
        return jiraName != null ? jiraName.getName() : null;
    }

    private String displayNameOf(JiraAssignee assignee) {
        return assignee != null ? assignee.getDisplayName() : null;
    }

    // ── ADF → Plain text ──────────────────────────────────────────────────────

    /**
     * Converts Atlassian Document Format (ADF) to plain text by recursively
     * collecting all "text" nodes.
     *
     * @param description raw ADF object from Jira API (typically Map or null)
     * @return plain text, or null if description is null
     */
    @SuppressWarnings("unchecked")
    private String extractPlainText(Object description) {
        if (description == null) {
            return null;
        }
        if (description instanceof String s) {
            return s;
        }
        if (description instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder();
            traverseAdf((Map<String, Object>) map, sb);
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

        // Traverse "content" array
        Object content = node.get("content");
        if (content instanceof List<?> contentList) {
            for (Object child : contentList) {
                if (child instanceof Map<?, ?> childMap) {
                    traverseAdf((Map<String, Object>) childMap, sb);
                }
            }
            // Add newline after block-level nodes
            if (isBlockNode(type)) {
                sb.append("\n");
            }
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

    // ── Config validation ─────────────────────────────────────────────────────

    private void validateConfig(IntegrationConfig config) {
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
    }

    // ── Inner context record ──────────────────────────────────────────────────

    private record JiraContext(String baseUrl, String jiraEmail, String token,
            String jql, int effectiveMax) {
    }
}
