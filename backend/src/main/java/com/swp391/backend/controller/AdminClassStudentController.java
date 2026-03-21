package com.swp391.backend.controller;

import com.swp391.backend.dto.request.AssignStudentToClassRequest;
import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.dto.response.UserResponse;
import com.swp391.backend.service.ClassStudentAssignmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/classes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminClassStudentController {

    private final ClassStudentAssignmentService classStudentAssignmentService;

    public AdminClassStudentController(ClassStudentAssignmentService classStudentAssignmentService) {
        this.classStudentAssignmentService = classStudentAssignmentService;
    }

    @PostMapping("/{classId}/students")
    public ApiResponse<Object> assignStudent(
            @PathVariable Long classId,
            @RequestBody AssignStudentToClassRequest request
    ) {
        classStudentAssignmentService.assignStudent(classId, request.getStudentId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    public ApiResponse<Object> unassignStudent(
            @PathVariable Long classId,
            @PathVariable Long studentId
    ) {
        classStudentAssignmentService.unassignStudent(classId, studentId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{classId}/students")
    public ApiResponse<List<UserResponse>> listStudents(@PathVariable Long classId) {
        return ApiResponse.success(classStudentAssignmentService.listStudentsInClass(classId));
    }
}