package com.swp391.backend.service;

import com.swp391.backend.entity.SyncLog;

public interface SyncLogService {
    SyncLog begin(Long groupId, String source);

    SyncLog updateStatus(Long logId, com.swp391.backend.entity.SyncStatus status, String message, Integer insertedCount,
                         Integer updatedCount);
}
