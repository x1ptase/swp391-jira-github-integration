package com.swp391.backend.dto.monitoring.response;

import com.swp391.backend.entity.monitoring.HealthStatus;
import com.swp391.backend.entity.monitoring.PrimaryReason;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupMonitoringDTO {
    // --- GROUP INFO---
    private final Long groupId;
    private final String groupName;
    private final String groupStatus;
    private final String topicName;
    private final int totalMembers;
    private final int activeMembers;
    private final String membersText;

    // --- TASK ---
    private final int overdueTasks;

    // --- COMMIT ---
    private final long totalCommits;
    private final LocalDateTime lastActivityAt;

    //  --- SYNC STATUS ---
    private final String githubSyncStatus;
    private final LocalDateTime githubSyncStartedAt;
    private final String jiraSyncStatus;
    private final LocalDateTime jiraSyncStartedAt;

    // --- HEALTH ---
    private final HealthStatus healthStatus;
    private final PrimaryReason primaryReason;
}
