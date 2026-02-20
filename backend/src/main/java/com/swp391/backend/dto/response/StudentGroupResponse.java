package com.swp391.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGroupResponse {
    @JsonProperty("group_id")
    private Long groupId;

    @JsonProperty("group_code")
    private String groupCode;

    @JsonProperty("group_name")
    private String groupName;

    @JsonProperty("course_code")
    private String courseCode;

    @JsonProperty("semester")
    private String semester;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("lecturer_id")
    private Long lecturerId;

    @JsonProperty("lecturer_name")
    private String lecturerName;
}
