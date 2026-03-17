package com.swp391.backend.controller;

import com.swp391.backend.common.ApiResponse;
import com.swp391.backend.entity.Semester;
import com.swp391.backend.service.SemesterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/semesters")
public class SemesterController {

    private final SemesterService semesterService;

    public SemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    // LIST SEMESTER
    @GetMapping
    public ApiResponse<Page<Semester>> listSemesters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
                semesterService.listSemesters(PageRequest.of(page, size))
        );
    }

    // GET DETAIL
    @GetMapping("/{id}")
    public ApiResponse<Semester> getSemester(@PathVariable Long id) {
        return ApiResponse.success(semesterService.getSemester(id));
    }

    // CREATE
    @PostMapping
    public ApiResponse<Semester> createSemester(@RequestBody Semester semester) {
        return ApiResponse.success(
                semesterService.createSemester(
                        semester.getSemesterCode(),
                        semester.getSemesterName(),
                        semester.getStartDate(),
                        semester.getEndDate()
                )
        );
    }

    // UPDATE
    @PutMapping("/{id}")
    public ApiResponse<Semester> updateSemester(
            @PathVariable Long id,
            @RequestBody Semester semester
    ) {
        return ApiResponse.success(
                semesterService.updateSemester(
                        id,
                        semester.getSemesterName(),
                        semester.getStartDate(),
                        semester.getEndDate()
                )
        );
    }
}