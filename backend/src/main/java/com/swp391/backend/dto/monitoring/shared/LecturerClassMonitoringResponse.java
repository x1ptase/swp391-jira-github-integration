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
public class LecturerClassMonitoringResponse {

    private Long classId;
    private String classCode;
    private String courseCode;
    private String semesterCode;
    private long totalGroups;
    private long groupsAtRisk;
    private long studentsFlagged;
    private String classHealth;
    private LocalDateTime lastUpdatedAt;
}