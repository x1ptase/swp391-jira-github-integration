package com.swp391.backend.service;

import com.swp391.backend.dto.response.AcademicClassResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AcademicClassService {
    Page<AcademicClassResponse> searchClasses(String keyword, String courseCode, String semesterCode, Pageable pageable);
}