package com.swp391.backend.service;

import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.entity.StudentGroup;

import java.util.List;

public interface LecturerAssignmentService {
    void assignLecturer(Long classId, Long lecturerId);
    List<AcademicClassResponse> getAssignedClassesForCurrentLecturer();
}
