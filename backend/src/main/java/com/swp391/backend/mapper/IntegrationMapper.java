package com.swp391.backend.mapper;

import com.swp391.backend.dto.response.IntegrationResponse;
import com.swp391.backend.dto.response.IntegrationResponseDTO;
import com.swp391.backend.dto.response.JiraIntegrationResponse;
import com.swp391.backend.entity.Integration;
import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.service.TokenCryptoService;
import com.swp391.backend.service.TokenHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegrationMapper {

    private final TokenCryptoService tokenCryptoService;
    private final TokenHelper tokenHelper;

    // ── Legacy Integration entity mapper (unchanged) ────────────────────────

    public IntegrationResponseDTO toResponseDTO(Integration integration) {
        if (integration == null) {
            return null;
        }

        boolean hasToken = integration.getEncryptedToken() != null
                && !integration.getEncryptedToken().isEmpty();
        String tokenMasked = null;

        if (hasToken) {
            try {
                String decryptedToken = tokenCryptoService.decrypt(integration.getEncryptedToken());
                tokenMasked = maskToken(decryptedToken);
            } catch (Exception e) {
                tokenMasked = "********";
            }
        }

        return IntegrationResponseDTO.builder()
                .id(integration.getId())
                .name(integration.getName())
                .source(integration.getSource())
                .hasToken(hasToken)
                .tokenMasked(tokenMasked)
                .build();
    }

    // ── GitHub config mapper (unchanged contract) ───────────────────────────

    public IntegrationResponse toResponse(IntegrationConfig config) {
        if (config == null) {
            return null;
        }

        boolean hasToken = config.getTokenEncrypted() != null
                && config.getTokenEncrypted().length > 0;
        String tokenMasked = null;

        if (hasToken) {
            try {
                String decryptedToken = tokenHelper.decryptFromBytes(config.getTokenEncrypted());
                tokenMasked = tokenHelper.maskToken(decryptedToken);
            } catch (Exception e) {
                tokenMasked = "********";
            }
        }

        return IntegrationResponse.builder()
                .repoFullName(config.getRepoFullName())
                .hasToken(hasToken)
                .tokenMasked(tokenMasked)
                .build();
    }

    // ── Jira config mapper ──────────────────────────────────────────────────

    /**
     * Maps an IntegrationConfig (Jira type) to JiraIntegrationResponse.
     * hasToken is computed from token_encrypted presence.
     * tokenMasked is derived by decrypting then masking – NEVER raw token returned.
     */
    public JiraIntegrationResponse toJiraResponse(IntegrationConfig config) {
        if (config == null) {
            return null;
        }

        boolean hasToken = config.getTokenEncrypted() != null
                && config.getTokenEncrypted().length > 0;
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

        return JiraIntegrationResponse.builder()
                .baseUrl(config.getBaseUrl())
                .projectKey(config.getProjectKey())
                .jiraEmail(config.getJiraEmail())
                .hasToken(hasToken)
                .tokenMasked(tokenMasked)
                .build();
    }

    // ── private helpers ─────────────────────────────────────────────────────

    private String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }
        if (token.length() <= 4) {
            return "****";
        }
        return "****" + token.substring(token.length() - 4);
    }
}
