package com.swp391.backend.controller;

import com.swp391.backend.common.ApiResponse;
import com.swp391.backend.service.ClassEnrollmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classes")
public class ClassEnrollmentController {

    private final ClassEnrollmentService classEnrollmentService;

    public ClassEnrollmentController(ClassEnrollmentService classEnrollmentService) {
        this.classEnrollmentService = classEnrollmentService;
    }

    @PostMapping("/{classId}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<Object> enroll(@PathVariable Long classId) {
        classEnrollmentService.enroll(classId);
        return ApiResponse.success(null);
    }
}