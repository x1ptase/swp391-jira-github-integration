package com.swp391.backend.service.impl;

import com.swp391.backend.entity.IntegrationConfig;
import com.swp391.backend.repository.IntegrationConfigRepository;
import com.swp391.backend.service.IntegrationService;
import com.swp391.backend.service.TokenHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IntegrationServiceImpl implements IntegrationService {

    private final IntegrationConfigRepository repository;
    private final TokenHelper tokenHelper;

    private static final Pattern REPO_PATTERN = Pattern.compile("^[a-zA-Z0-9-._]+/[a-zA-Z0-9-._]+$");

    @Override
    @Transactional
    public IntegrationConfig saveOrUpdate(Long groupId, String repoFullName, String token) {
        // 1. Validate repoFullName
        if (repoFullName == null || !REPO_PATTERN.matcher(repoFullName).matches()) {
            throw new IllegalArgumentException("Invalid repoFullName format. Expected: org/repo");
        }

        Optional<IntegrationConfig> existingConfig = repository.findByGroupId(groupId);

        if (existingConfig.isPresent()) {
            // Update logic
            IntegrationConfig config = existingConfig.get();
            config.setRepoFullName(repoFullName);

            // Only update token if a new one is provided
            if (token != null && !token.trim().isEmpty()) {
                config.setTokenEncrypted(tokenHelper.encrypt(token));
            }

            return repository.save(config);
        } else {
            // Create logic
            if (token == null || token.trim().isEmpty()) {
                throw new IllegalArgumentException("Token is required for new integration configuration");
            }

            IntegrationConfig newConfig = IntegrationConfig.builder()
                    .groupId(groupId)
                    .repoFullName(repoFullName)
                    .tokenEncrypted(tokenHelper.encrypt(token))
                    .build();

            return repository.save(newConfig);
        }
    }
}
