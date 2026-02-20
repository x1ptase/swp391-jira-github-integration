package com.swp391.backend.controller;

import com.swp391.backend.common.ApiResponse;
import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.service.impl.StudentGroupServiceImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RequestMapping("/api/student_group")
@RestController
public class StudentGroupController {

    private final StudentGroupServiceImpl studentGroupService;

    public StudentGroupController(StudentGroupServiceImpl studentGroupService) {
        this.studentGroupService = studentGroupService;
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentGroup> createStudentGroup(@Valid @RequestBody StudentGroup studentGroup) {
        StudentGroup stu = studentGroupService.addStudentGroup(studentGroup);
        return ApiResponse.success(stu);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentGroup> updateStudentGroup(@Valid @RequestBody StudentGroup studentGroup,
            @PathVariable Long id) {
        StudentGroup stu = studentGroupService.updateStudentGroup(studentGroup, id);
        return ApiResponse.success(stu);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentGroup> deleteStudentGroup(@PathVariable Long id) {
        StudentGroup stu = studentGroupService.deleteStudentGroup(id);
        return ApiResponse.success(stu);
    }

    @GetMapping({ "", "/" })
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    public ApiResponse<List<StudentGroup>> listStudentGroups(
            @RequestParam(value = "course_code", required = false) String courseCode,
            @RequestParam(value = "semester", required = false) String semester) {
        return ApiResponse.success(studentGroupService.listStudentGroups(courseCode, semester));
    }

}
