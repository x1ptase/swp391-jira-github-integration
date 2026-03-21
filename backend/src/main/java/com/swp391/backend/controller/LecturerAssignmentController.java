package com.swp391.backend.controller;

import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.dto.request.AssignLecturerRequest;
import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.dto.response.LecturerGroupSummaryResponse;
import com.swp391.backend.dto.response.StudentClassDetailsResponse;
import com.swp391.backend.service.ClassStudentAssignmentService;
import com.swp391.backend.service.LecturerAssignmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LecturerAssignmentController {

    private final LecturerAssignmentService lecturerAssignmentService;
    private final ClassStudentAssignmentService classStudentAssignmentService;

    public LecturerAssignmentController(
            LecturerAssignmentService lecturerAssignmentService,
            ClassStudentAssignmentService classStudentAssignmentService) {
        this.lecturerAssignmentService = lecturerAssignmentService;
        this.classStudentAssignmentService = classStudentAssignmentService;
    }

    // Admin assign 1 lecturer for class
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/classes/{classId}/lecturer")
    public ApiResponse<Object> assignLecturer(@PathVariable Long classId,
                                              @Valid @RequestBody AssignLecturerRequest req) {
        lecturerAssignmentService.assignLecturer(classId, req.getLecturerId());
        return ApiResponse.success(null);
    }

    // Lecturer xem các class được assign
    @PreAuthorize("hasRole('LECTURER')")
    @GetMapping("/lecturer/classes")
    public ApiResponse<List<AcademicClassResponse>> myAssignedClasses() {
        return ApiResponse.success(lecturerAssignmentService.getAssignedClassesForCurrentLecturer());
    }

    /**
     * Lấy danh sách tất cả StudentGroup mà Giảng viên hiện tại phụ trách,
     * kèm thông tin topic và danh sách thành viên của từng nhóm.
     *
     * <p>Phân quyền: chỉ LECTURER mới được gọi endpoint này.</p>
     *
     * @return danh sách {@link LecturerGroupSummaryResponse}
     */
    @PreAuthorize("hasRole('LECTURER')")
    @GetMapping("/lecturer/groups")
    public ApiResponse<List<LecturerGroupSummaryResponse>> myGroups() {
        return ApiResponse.success(lecturerAssignmentService.getGroupsForCurrentLecturer());
    }

    /**
     * Retrieves the paginated list of students in a specific class for the lecturer,
     * with optional filtering by search (name/studentCode) and hasGroup status.
     */
    @PreAuthorize("hasRole('LECTURER')")
    @GetMapping("/lecturer/classes/{classId}/students")
    public ApiResponse<Page<StudentClassDetailsResponse>> getStudentsForClass(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean hasGroup) {

        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(classStudentAssignmentService.searchStudents(classId, search, hasGroup, pageable));
    }
}
