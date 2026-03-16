package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicClassResponse {
    private Long classId;
    private String classCode;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private Long semesterId;
    private String semesterCode;
    private String semesterName;
}