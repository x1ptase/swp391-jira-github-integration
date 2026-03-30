package com.swp391.backend.dto.monitoring.response;

import com.swp391.backend.entity.monitoring.ContributionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudentWatchlistDTO {

    // --- STUDENT INFO ---
    private final Long userId;
    private final String fullName;
    private final String studentCode;
    private final String email;

    // --- GROUP INFO ---
    private final Long groupId;
    private final String groupName;
    private final String memberRole;

    // --- COMMIT ---
    private final long commitCount;
    private final LocalDateTime lastActiveAt;

    // --- CONTRIBUTOR TYPE ---
    private final ContributionStatus contributionStatus;
}
