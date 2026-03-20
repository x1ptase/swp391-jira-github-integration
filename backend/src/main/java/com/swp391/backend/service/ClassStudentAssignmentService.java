package com.swp391.backend.service;

import com.swp391.backend.dto.response.UserResponse;

import java.util.List;

public interface ClassStudentAssignmentService {
    void assignStudent(Long classId, Long studentId);
    void unassignStudent(Long classId, Long studentId);
    List<UserResponse> listStudentsInClass(Long classId);
}