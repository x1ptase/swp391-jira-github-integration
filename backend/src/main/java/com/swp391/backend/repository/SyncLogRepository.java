package com.swp391.backend.repository;

import com.swp391.backend.entity.SyncLog;
import com.swp391.backend.entity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
    Optional<SyncLog> findByGroupIdAndSourceAndStatus(Long groupId, String source, SyncStatus status);
}
