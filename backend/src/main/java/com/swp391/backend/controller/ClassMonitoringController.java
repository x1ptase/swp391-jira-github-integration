package com.swp391.backend.controller;

import com.swp391.backend.dto.monitoring.request.MonitoringFilterRequest;
import com.swp391.backend.dto.monitoring.response.GroupMonitoringDTO;
import com.swp391.backend.dto.monitoring.response.StudentWatchlistDTO;
import com.swp391.backend.dto.monitoring.shared.ClassMonitoringSummaryResponse;
import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.entity.monitoring.ContributionStatus;
import com.swp391.backend.entity.monitoring.HealthStatus;
import com.swp391.backend.repository.AcademicClassRepository;
import com.swp391.backend.service.monitoring.ClassMonitoringService;
import com.swp391.backend.service.monitoring.GroupMonitoringService;
import com.swp391.backend.service.monitoring.StudentMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Class Monitoring")
@RestController
@RequestMapping("/api/classes/{classId}/monitoring")
@RequiredArgsConstructor
public class ClassMonitoringController {

    private final ClassMonitoringService classMonitoringService;
    private final GroupMonitoringService groupMonitoringService;
    private final StudentMonitoringService studentMonitoringService;
    private final AcademicClassRepository academicClassRepository;

    // ════════════════════════════════════════════════════════
    // BE-02: GET /api/classes/{classId}/monitoring/summary
    // ════════════════════════════════════════════════════════
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    public ResponseEntity<ApiResponse<ClassMonitoringSummaryResponse>> getClassMonitoringSummary(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        classMonitoringService.getSummary(classId, fromDate, toDate)));
    }

    // ════════════════════════════════════════════════════════
    // GET /api/classes/{classId}/monitoring/groups
    // ════════════════════════════════════════════════════════
    @Operation(
            summary = "Bảng Group Status Monitoring",
            description = """
            Trả về danh sách sức khoẻ của tất cả nhóm trong lớp.
            Hỗ trợ lọc theo healthStatus và tìm kiếm theo tên nhóm.
            Kết quả sắp xếp: CRITICAL → WARNING → CLOSED → HEALTHY.
            Chỉ dành cho ADMIN hoặc LECTURER phụ trách lớp đó.
            """
    )
    @GetMapping("/groups")
    @PreAuthorize("@securityService.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<List<GroupMonitoringDTO>>> getGroupMonitoring(
            @Parameter(description = "ID lớp học") @PathVariable Long classId,
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Số ngày gần đây, ghi đè fromDate/toDate (>= 1)")
            @RequestParam(required = false) Integer lastNDays,
            @Parameter(description = "Lọc health status: HEALTHY | WARNING | CRITICAL | CLOSED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Tìm kiếm theo tên nhóm (case-insensitive)")
            @RequestParam(required = false) String keyword
    ) {
        requireClassExists(classId);
        validateHealthStatus(status);

        MonitoringFilterRequest filter = MonitoringFilterRequest.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .lastNDays(lastNDays)
                .status(status)
                .keyword(keyword)
                .build();

        List<GroupMonitoringDTO> results = groupMonitoringService.getByClass(classId, filter);

        if (results.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(200, "Không có nhóm nào phù hợp với điều kiện lọc.", List.of()));
        }

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ════════════════════════════════════════════════════════
    // GET /api/classes/{classId}/monitoring/students
    // ════════════════════════════════════════════════════════
    @Operation(
            summary = "Bảng Students Watchlist",
            description = """
            Trả về danh sách sinh viên trong lớp với thống kê commit và phân loại đóng góp.
            Hỗ trợ lọc theo nhóm, contributionStatus, tìm kiếm tên/mã SV, và phân trang.
            Kết quả ưu tiên hiển thị: NO_CONTRIBUTION → LOW → ACTIVE.
            Chỉ dành cho ADMIN hoặc LECTURER phụ trách lớp đó.
            """
    )
    @GetMapping("/students")
    @PreAuthorize("@securityService.hasAccessToClass(#classId)")
    public ResponseEntity<ApiResponse<List<StudentWatchlistDTO>>> getStudentWatchlist(
            @Parameter(description = "ID lớp học") @PathVariable Long classId,
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Số ngày gần đây, ghi đè fromDate/toDate (>= 1)")
            @RequestParam(required = false) Integer lastNDays,
            @Parameter(description = "Lọc sinh viên thuộc nhóm cụ thể")
            @RequestParam(required = false) Long groupId,
            @Parameter(description = "Lọc contribution status: ACTIVE | LOW | NO_CONTRIBUTION")
            @RequestParam(required = false) String status,
            @Parameter(description = "Tìm kiếm theo tên hoặc mã sinh viên (case-insensitive)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Số trang (0-indexed, mặc định 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Kích thước trang (mặc định 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        requireClassExists(classId);
        validateContributionStatus(status);

        MonitoringFilterRequest filter = MonitoringFilterRequest.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .lastNDays(lastNDays)
                .groupId(groupId)
                .status(status)
                .keyword(keyword)
                .page(page)
                .size(size)
                .build();

        List<StudentWatchlistDTO> results = studentMonitoringService.getWatchlistByClass(classId, filter);

        if (results.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(200, "Không có sinh viên nào phù hợp với điều kiện lọc.", List.of()));
        }

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ════════════════════════════════════════════════════════
    // Private helpers
    // ════════════════════════════════════════════════════════
    private void requireClassExists(Long classId) {
        if (!academicClassRepository.existsById(classId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy lớp học với id = " + classId);
        }
    }

    private void validateHealthStatus(String status) {
        if (status == null || status.isBlank()) return;
        try {
            HealthStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giá trị status không hợp lệ: '" + status
                            + "'. Chấp nhận: HEALTHY, WARNING, CRITICAL, CLOSED");
        }
    }

    private void validateContributionStatus(String status) {
        if (status == null || status.isBlank()) return;
        try {
            ContributionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giá trị status không hợp lệ: '" + status
                            + "'. Chấp nhận: ACTIVE, LOW, NO_CONTRIBUTION");
        }
    }
}