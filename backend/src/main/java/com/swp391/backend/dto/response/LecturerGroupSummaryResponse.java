package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LecturerGroupSummaryResponse {
    private Long groupId;
    private String groupName;
    private String classCode;
    private String semesterCode;
    private String topicName;
    private String studentCode;
    private List<GroupMemberResponse> members;
}
