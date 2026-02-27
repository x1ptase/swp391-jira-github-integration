package com.swp391.backend.service;

import com.swp391.backend.entity.IntegrationConfig;

public interface IntegrationService {
    IntegrationConfig saveOrUpdate(Long groupId, String repoFullName, String token);
}
