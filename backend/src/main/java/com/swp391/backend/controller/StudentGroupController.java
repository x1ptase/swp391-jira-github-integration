package com.swp391.backend.controller;

import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.dto.request.AssignTopicRequest;
import com.swp391.backend.dto.request.ChangeGroupStatusRequest;
import com.swp391.backend.dto.request.CreateGroupRequest;
import com.swp391.backend.dto.response.StudentGroupResponse;
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
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<StudentGroupResponse> createStudentGroup(@Valid @RequestBody CreateGroupRequest request) {
        StudentGroupResponse stu = studentGroupService.addStudentGroup(request);
        return ApiResponse.success(stu);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<StudentGroupResponse> updateStudentGroup(@Valid @RequestBody StudentGroup studentGroup,
            @PathVariable Long id) {
        StudentGroupResponse stu = studentGroupService.updateStudentGroup(studentGroup, id);
        return ApiResponse.success(stu);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<StudentGroupResponse> deleteStudentGroup(@PathVariable Long id) {
        StudentGroupResponse stu = studentGroupService.deleteStudentGroup(id);
        return ApiResponse.success(stu);
    }

    @GetMapping({ "", "/" })
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER', 'STUDENT')")
    public ApiResponse<List<StudentGroupResponse>> listStudentGroups(
            @RequestParam(value = "course_code", required = false) String courseCode,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam(value = "class_id", required = false) Long classId) {
        return ApiResponse.success(studentGroupService.listStudentGroups(courseCode, semester, classId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<StudentGroupResponse> getMyGroup() {
        return ApiResponse.success(studentGroupService.getMyGroup());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<StudentGroupResponse> changeGroupStatus(@PathVariable Long id,
            @Valid @RequestBody ChangeGroupStatusRequest request) {
        return ApiResponse.success(studentGroupService.changeGroupStatus(id, request.getStatus()));
    }

    @PutMapping("/{id}/topic")
    @PreAuthorize("hasRole('LECTURER')")
    public ApiResponse<StudentGroupResponse> assignTopic(@PathVariable Long id,
            @RequestBody AssignTopicRequest request) {
        return ApiResponse.success(studentGroupService.assignTopic(id, request.getTopicId()));
    }
}
