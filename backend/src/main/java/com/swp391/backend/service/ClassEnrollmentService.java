package com.swp391.backend.service;

import com.swp391.backend.dto.response.AcademicClassResponse;

public interface ClassEnrollmentService {
    void enroll(Long classId);

    AcademicClassResponse getMyClass();
}