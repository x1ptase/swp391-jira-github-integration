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

    private Long groupId;


    private String groupCode;


    private String groupName;


    private String courseCode;


    private String semester;


    private LocalDateTime createdAt;


    private Long lecturerId;


    private String lecturerName;
}
