package com.swp391.backend.dto.monitoring.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassMonitoringSummaryResponse {
    private Long classId;
    private String classCode;
    private long totalGroups;
    private long atRisk;
    private long studentsFlagged;
    private LocalDateTime lastUpdatedAt;
}