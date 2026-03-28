package com.swp391.backend.controller;

import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.entity.AcademicClass;
import com.swp391.backend.service.AcademicClassService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import com.swp391.backend.dto.response.ClassSummaryResponse;

@RestController
@RequestMapping("/api/classes")
public class AcademicClassController {

    private final AcademicClassService academicClassService;

    public AcademicClassController(AcademicClassService academicClassService) {
        this.academicClassService = academicClassService;
    }

    //SUMMARY CLASS
    @GetMapping("/{id}/summary")
    public ApiResponse<ClassSummaryResponse> getClassSummary(@PathVariable Long id) {
        return ApiResponse.success(academicClassService.getClassSummary(id));
    }

    // SEARCH CLASS
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

    // GET CLASS DETAIL
    @GetMapping("/{id}")
    public ApiResponse<AcademicClassResponse> getClass(@PathVariable Long id) {
        return ApiResponse.success(academicClassService.getClass(id));
    }

    // CREATE CLASS (JSON BODY)
    @PostMapping
    public ApiResponse<AcademicClassResponse> createClass(
            @RequestBody AcademicClass academicClass
    ) {
        return ApiResponse.success(
                academicClassService.createClass(
                        academicClass.getClassCode(),
                        academicClass.getCourse().getCourseId(),
                        academicClass.getSemester().getSemesterId()
                )
        );
    }

    // UPDATE CLASS (JSON BODY)
    @PutMapping("/{id}")
    public ApiResponse<AcademicClassResponse> updateClass(
            @PathVariable Long id,
            @RequestBody AcademicClass academicClass
    ) {
        return ApiResponse.success(
                academicClassService.updateClass(
                        id,
                        academicClass.getClassCode(),
                        academicClass.getCourse().getCourseId(),
                        academicClass.getSemester().getSemesterId()
                )
        );
    }

    // DELETE CLASS
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteClass(@PathVariable Long id) {
        academicClassService.deleteClass(id);
        return ApiResponse.success("Class deleted successfully");
    }
}