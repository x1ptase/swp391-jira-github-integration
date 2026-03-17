package com.swp391.backend.service;

import com.swp391.backend.dto.response.AcademicClassResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AcademicClassService {
    AcademicClassResponse createClass(String classCode, Long courseId, Long semesterId);

    Page<AcademicClassResponse> searchClasses(String keyword, String courseCode, String semesterCode, Pageable pageable);

    AcademicClassResponse getClass(Long id);

    AcademicClassResponse updateClass(Long id, String classCode, Long courseId, Long semesterId);

    void deleteClass(Long id);
}