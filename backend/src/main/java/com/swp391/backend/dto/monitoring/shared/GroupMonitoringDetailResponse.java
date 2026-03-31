package com.swp391.backend.dto.monitoring.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMonitoringDetailResponse {

    private Long groupId;
    private String groupName;
    private Long classId;
    private String classCode;
    private String topicName;
    private String operationalStatus;
    private String health;
    private SummaryDTO summary;
    private List<String> reasons;
    private List<MemberDTO> members;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryDTO {
        private long commits;
        private long activeMembers;
        private long totalMembers;
        private long overdueTasks;
        private LocalDateTime lastActivityAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberDTO {
        private Long userId;
        private String fullName;
        private String role;
        private long commitCount;
        private LocalDateTime lastActiveAt;
        private String contributionStatus;
    }
}