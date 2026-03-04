package com.swp391.backend.service.impl;

import com.swp391.backend.common.IntegrationTypeIds;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.integration.jira.JiraClient;
import com.swp391.backend.integration.jira.dto.JiraFields;
import com.swp391.backend.integration.jira.dto.JiraIssue;
import com.swp391.backend.integration.jira.dto.JiraBulkFetchResponse;
import com.swp391.backend.integration.jira.dto.JiraIssueIdRef;
import com.swp391.backend.integration.jira.dto.JiraSearchJqlResponse;
import com.swp391.backend.repository.IntegrationConfigRepository;
import com.swp391.backend.service.JiraLabelService;
import com.swp391.backend.service.TokenCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JiraLabelServiceImpl implements JiraLabelService {

    /** Hard cap: tổng số issues tối đa để aggregate labels. */
    private static final int HARD_CAP_ISSUES = 200;
    /** Page size khi fetch issues. */
    private static final int PAGE_SIZE = 50;
    /** Max loops để tránh infinite loop. */
    private static final int MAX_LOOPS = 10;

    private final IntegrationConfigRepository integrationConfigRepository;
    private final TokenCryptoService tokenCryptoService;
    private final JiraClient jiraClient;

    @Override
    @Transactional(readOnly = true)
    public List<String> suggestLabels(Long groupId, String q, int limit) {
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

        // 4. Normalize inputs
        String baseUrl = cfg.getBaseUrl().stripTrailing().replaceAll("/+$", "");
        int effectiveLimit = Math.max(1, Math.min(limit, 100));

        // 5. Aggregate labels from issues
        Set<String> allLabels = new LinkedHashSet<>();

        // Attempt with q-filtered JQL first (prefix label search), fallback to base JQL
        String baseJql = "project = \"" + cfg.getProjectKey() + "\" ORDER BY updated DESC";

        try {
            allLabels = aggregateLabels(baseUrl, cfg.getJiraEmail(), rawToken, baseJql);
        } catch (BusinessException e) {
            // Fallback: already using base JQL, rethrow if still failing
            throw e;
        }

        // 6. Apply q filter locally (case-insensitive contains)
        String qNorm = (q == null) ? "" : q.trim().toLowerCase();
        List<String> filtered;
        if (qNorm.isBlank()) {
            filtered = new ArrayList<>(allLabels);
        } else {
            final String qFinal = qNorm;
            filtered = allLabels.stream()
                    .filter(label -> label.toLowerCase().contains(qFinal))
                    .collect(Collectors.toList());
        }

        // 7. Return top {limit}
        return filtered.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(effectiveLimit)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Paginate issues với JQL, extract labels, stop khi đủ HARD_CAP_ISSUES hoặc
     * isLast.
     */
    private Set<String> aggregateLabels(String baseUrl, String jiraEmail, String rawToken, String jql) {
        Set<String> labels = new LinkedHashSet<>();
        String nextPageToken = null;
        int totalFetched = 0;
        int loops = 0;

        while (totalFetched < HARD_CAP_ISSUES && loops < MAX_LOOPS) {
            // Search: chỉ lấy IDs
            JiraSearchJqlResponse searchResp = jiraClient.searchIssuesForLabels(
                    baseUrl, jiraEmail, rawToken, jql, PAGE_SIZE, nextPageToken);

            List<JiraIssueIdRef> refs = searchResp.getIssues();
            if (refs == null || refs.isEmpty())
                break;

            List<String> ids = refs.stream()
                    .filter(r -> r.getId() != null)
                    .map(JiraIssueIdRef::getId)
                    .toList();

            if (!ids.isEmpty()) {
                // Bulk fetch để lấy labels field
                JiraBulkFetchResponse bulkResp = jiraClient.bulkFetchIssueDetails(
                        baseUrl, jiraEmail, rawToken, ids);

                List<JiraIssue> issues = bulkResp.getIssues();
                if (issues != null) {
                    for (JiraIssue issue : issues) {
                        JiraFields fields = issue.getFields();
                        if (fields != null && fields.getLabels() != null) {
                            labels.addAll(fields.getLabels());
                        }
                    }
                }
                totalFetched += ids.size();
            }

            boolean isLast = Boolean.TRUE.equals(searchResp.getIsLast())
                    || searchResp.getNextPageToken() == null;
            if (isLast)
                break;

            nextPageToken = searchResp.getNextPageToken();
            loops++;
        }

        return labels;
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
