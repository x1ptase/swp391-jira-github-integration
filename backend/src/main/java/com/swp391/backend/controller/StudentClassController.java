package com.swp391.backend.controller;

import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.service.StudentClassQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes")
public class StudentClassController {

    private final StudentClassQueryService studentClassQueryService;

    public StudentClassController(StudentClassQueryService studentClassQueryService) {
        this.studentClassQueryService = studentClassQueryService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<AcademicClassResponse> getMyClass() {
        return ApiResponse.success(studentClassQueryService.getMyClass());
    }
}