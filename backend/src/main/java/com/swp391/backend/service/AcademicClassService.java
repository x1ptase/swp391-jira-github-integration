package com.swp391.backend.service;

import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.dto.response.ClassSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AcademicClassService {
    AcademicClassResponse createClass(String classCode, Long courseId, Long semesterId);

    Page<AcademicClassResponse> searchClasses(String keyword, String courseCode, String semesterCode, Pageable pageable);

    AcademicClassResponse getClass(Long id);

    AcademicClassResponse updateClass(Long id, String classCode, Long courseId, Long semesterId);

    ClassSummaryResponse getClassSummary(Long classId);

    void deleteClass(Long id);
}