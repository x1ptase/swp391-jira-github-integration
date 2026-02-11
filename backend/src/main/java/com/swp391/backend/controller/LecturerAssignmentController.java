package com.swp391.backend.controller;

import com.swp391.backend.common.ApiResponse;
import com.swp391.backend.dto.request.AssignLecturerRequest;
import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.service.LecturerAssignmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LecturerAssignmentController {

    private final LecturerAssignmentService lecturerAssignmentService;

    public LecturerAssignmentController(LecturerAssignmentService lecturerAssignmentService) {
        this.lecturerAssignmentService = lecturerAssignmentService;
    }

    // Admin assign 1 lecturer for group
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/groups/{groupId}/lecturer")
    public ApiResponse<Object> assignLecturer(@PathVariable Long groupId,
                                              @Valid @RequestBody AssignLecturerRequest req) {
        lecturerAssignmentService.assignLecturer(groupId, req.getLecturerId());
        return ApiResponse.success(null);
    }

    // Lecturer chỉ xem các group được assign
    @PreAuthorize("hasRole('LECTURER')")
    @GetMapping("/lecturer/groups")
    public ApiResponse<List<StudentGroup>> myAssignedGroups() {
        return ApiResponse.success(lecturerAssignmentService.getAssignedGroupsForCurrentLecturer());
    }
}

