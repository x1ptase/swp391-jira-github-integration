package com.swp391.backend.mapper;

import com.swp391.backend.dto.response.IntegrationResponse;
import com.swp391.backend.dto.response.JiraIntegrationResponse;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.service.TokenHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegrationMapper {

    private final TokenHelper tokenHelper;

    // ── GitHub config mapper (unchanged contract) ───────────────────────────

    public IntegrationResponse toResponse(IntegrationConfig config) {
        if (config == null) {
            return null;
        }

        boolean hasToken = config.getTokenEncrypted() != null && config.getTokenEncrypted().length > 0;
        String tokenMasked = null;

        if (hasToken) {
            try {
                String decryptedToken = tokenHelper.decryptFromBytes(config.getTokenEncrypted());
                tokenMasked = tokenHelper.maskToken(decryptedToken);
            } catch (Exception e) {
                tokenMasked = "********";
            }
        }

        return IntegrationResponse.builder().repoFullName(config.getRepoFullName()).hasToken(hasToken)
                .tokenMasked(tokenMasked).build();
    }

    // ── Jira config mapper ──────────────────────────────────────────────────

    public JiraIntegrationResponse toJiraResponse(IntegrationConfig config) {
        if (config == null) {
            return null;
        }

        boolean hasToken = config.getTokenEncrypted() != null && config.getTokenEncrypted().length > 0;
        String tokenMasked = null;

        if (hasToken) {
            try {
                String decryptedToken = tokenHelper.decryptFromBytes(config.getTokenEncrypted());
                tokenMasked = tokenHelper.maskToken(decryptedToken);
            } catch (Exception e) {
                // Decryption failure: return safe fallback, never raw bytes
                tokenMasked = "********";
            }
        }

        return JiraIntegrationResponse.builder().baseUrl(config.getBaseUrl()).projectKey(config.getProjectKey())
                .jiraEmail(config.getJiraEmail()).hasToken(hasToken).tokenMasked(tokenMasked).build();
    }

}
