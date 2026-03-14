package com.swp391.backend.controller;

import com.swp391.backend.common.ApiResponse;
import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.service.AcademicClassService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classes")
public class AcademicClassController {

    private final AcademicClassService academicClassService;

    public AcademicClassController(AcademicClassService academicClassService) {
        this.academicClassService = academicClassService;
    }

    @GetMapping
    public ApiResponse<Page<AcademicClassResponse>> searchClasses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String semesterCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<AcademicClassResponse> result = academicClassService.searchClasses(
                keyword, courseCode, semesterCode, PageRequest.of(page, size)
        );
        return ApiResponse.success(result);
    }
}