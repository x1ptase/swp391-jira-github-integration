package com.swp391.backend.service.impl;

import com.swp391.backend.common.IntegrationTypeIds;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.IntegrationConfigRepository;
import com.swp391.backend.service.IntegrationService;
import com.swp391.backend.service.TokenHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IntegrationServiceImpl implements IntegrationService {

    private final IntegrationConfigRepository repository;
    private final TokenHelper tokenHelper;

    // ── GitHub ──────────────────────────────────────────────────────────────

    private static final Pattern REPO_PATTERN = Pattern.compile("^[a-zA-Z0-9-._]+/[a-zA-Z0-9-._]+$");

    @Override
    @Transactional
    public IntegrationConfig saveOrUpdate(Long groupId, String repoFullName, String token) {
        // 1. Validate repoFullName
        if (repoFullName == null || !REPO_PATTERN.matcher(repoFullName).matches()) {
            throw new BusinessException("Invalid repoFullName format. Expected: owner/repo", 400);
        }

        Optional<IntegrationConfig> existingConfig = repository.findByGroupIdAndIntegrationTypeId(groupId,
                IntegrationTypeIds.GITHUB);

        if (existingConfig.isPresent()) {
            IntegrationConfig config = existingConfig.get();
            config.setRepoFullName(repoFullName);

            if (token != null && !token.trim().isEmpty()) {
                config.setTokenEncrypted(tokenHelper.encryptToBytes(token));
            }

            return repository.save(config);
        } else {
            if (token == null || token.trim().isEmpty()) {
                throw new BusinessException("Token is required for new GitHub integration configuration", 400);
            }

            IntegrationConfig newConfig = IntegrationConfig.builder()
                    .groupId(groupId)
                    .integrationTypeId(IntegrationTypeIds.GITHUB)
                    .repoFullName(repoFullName)
                    .tokenEncrypted(tokenHelper.encryptToBytes(token))
                    .build();

            return repository.save(newConfig);
        }
    }

    // ── Jira ─────────────────────────────────────────────────────────────────

    /**
     * Simple email regex (Jakarta @Email covers annotation-validated fields, this
     * covers service-layer checks).
     */
    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    @Override
    @Transactional
    public IntegrationConfig saveOrUpdateJira(Long groupId, String baseUrl, String projectKey,
            String jiraEmail, String token) {

        // 1. Validate baseUrl – must be http/https URL with a host
        validateBaseUrl(baseUrl);

        // 2. Validate projectKey not blank
        if (projectKey == null || projectKey.isBlank()) {
            throw new BusinessException("projectKey must not be blank", 400);
        }

        // 3. Validate jiraEmail
        if (jiraEmail == null || jiraEmail.isBlank() || !EMAIL_PATTERN.matcher(jiraEmail).matches()) {
            throw new BusinessException("jiraEmail must be a valid email address", 400);
        }

        Optional<IntegrationConfig> existingOpt = repository.findByGroupIdAndIntegrationTypeId(groupId,
                IntegrationTypeIds.JIRA);

        if (existingOpt.isPresent()) {
            // ── UPDATE ──
            IntegrationConfig config = existingOpt.get();
            config.setBaseUrl(baseUrl);
            config.setProjectKey(projectKey);
            config.setJiraEmail(jiraEmail);

            // Only update token_encrypted when a new token is supplied
            if (token != null && !token.trim().isEmpty()) {
                config.setTokenEncrypted(tokenHelper.encryptToBytes(token));
            }

            return repository.save(config);
        } else {
            // ── CREATE ──
            // Token is mandatory on create; do NOT log or include token value in error
            // message
            if (token == null || token.trim().isEmpty()) {
                throw new BusinessException("Token is required when creating a new Jira integration configuration",
                        400);
            }

            IntegrationConfig newConfig = IntegrationConfig.builder()
                    .groupId(groupId)
                    .integrationTypeId(IntegrationTypeIds.JIRA)
                    .baseUrl(baseUrl)
                    .projectKey(projectKey)
                    .jiraEmail(jiraEmail)
                    .tokenEncrypted(tokenHelper.encryptToBytes(token))
                    .build();

            return repository.save(newConfig);
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void validateBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BusinessException("baseUrl must not be blank", 400);
        }
        try {
            URL url = new URL(baseUrl);
            String protocol = url.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new BusinessException("baseUrl must use http or https scheme", 400);
            }
            if (url.getHost() == null || url.getHost().isBlank()) {
                throw new BusinessException("baseUrl must contain a valid host", 400);
            }
        } catch (MalformedURLException e) {
            throw new BusinessException("baseUrl is not a valid URL", 400);
        }
    }
}
