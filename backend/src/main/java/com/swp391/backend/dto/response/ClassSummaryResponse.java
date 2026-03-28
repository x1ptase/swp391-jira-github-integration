package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSummaryResponse {
    private Long classId;
    private String classCode;

    private long totalStudents;
    private long totalGroups;

    private long groupsWithAssignedTopic;
    private String topicAssignedSummary; // ví dụ "4/6"

    private long studentsWithoutGroup;
}