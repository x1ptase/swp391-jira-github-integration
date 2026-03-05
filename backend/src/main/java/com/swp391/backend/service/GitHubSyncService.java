package com.swp391.backend.service;

import com.swp391.backend.dto.response.SyncResultResponse;

public interface GitHubSyncService {
    SyncResultResponse syncNow(Long groupId);
}
