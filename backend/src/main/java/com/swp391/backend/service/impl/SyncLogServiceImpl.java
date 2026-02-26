package com.swp391.backend.service.impl;

import com.swp391.backend.entity.SyncLog;
import com.swp391.backend.entity.SyncStatus;
import com.swp391.backend.repository.SyncLogRepository;
import com.swp391.backend.service.SyncLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SyncLogServiceImpl implements SyncLogService {

    private final SyncLogRepository syncLogRepository;

    public SyncLogServiceImpl(SyncLogRepository syncLogRepository) {
        this.syncLogRepository = syncLogRepository;
    }

    @Override
    @Transactional
    public SyncLog begin(Long groupId, String source) {
        syncLogRepository.findByGroupIdAndSourceAndStatus(groupId, source, SyncStatus.RUNNING)
                .ifPresent(log -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Sync is running");
                });

        SyncLog syncLog = SyncLog.builder()
                .groupId(groupId)
                .source(source)
                .status(SyncStatus.RUNNING)
                .build();

        return syncLogRepository.save(syncLog);
    }

    @Override
    @Transactional
    public SyncLog updateStatus(Long logId, SyncStatus status, String message, Integer insertedCount,
                                Integer updatedCount) {
        SyncLog syncLog = syncLogRepository.findById(logId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SyncLog not found"));

        syncLog.setStatus(status);
        syncLog.setMessage(message);
        syncLog.setInsertedCount(insertedCount);
        syncLog.setUpdatedCount(updatedCount);

        return syncLogRepository.save(syncLog);
    }
}
