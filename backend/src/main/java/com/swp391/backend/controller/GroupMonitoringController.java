package com.swp391.backend.controller;

import com.swp391.backend.dto.monitoring.request.MonitoringFilterRequest;
import com.swp391.backend.dto.monitoring.response.GroupMonitoringDTO;
import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.entity.monitoring.HealthStatus;
import com.swp391.backend.repository.AcademicClassRepository;
import com.swp391.backend.service.monitoring.GroupMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller phục vụ bảng Group Status Monitoring.
 *
 * <h3>Endpoint chính</h3>
 * <pre>GET /api/classes/{classId}/monitoring/groups</pre>
 *
 * <h3>Phân quyền</h3>
 * <ul>
 *   <li><b>ADMIN</b> – luôn được phép.</li>
 *   <li><b>LECTURER</b> – chỉ được phép nếu đã được gán vào lớp đó
 *       ({@code LecturerAssignment}).</li>
 *   <li>Các role khác → {@code 403 Forbidden}.</li>
 * </ul>
 *
 * <h3>Validation thứ tự</h3>
 * <ol>
 *   <li>Spring Security kiểm tra JWT và role cơ bản.</li>
 *   <li>{@code @PreAuthorize} gọi {@code securityService.hasAccessToClass()} để
 *       kiểm tra quyền cụ thể theo classId.</li>
 *   <li>Controller kiểm tra classId tồn tại trong DB → {@code 404} nếu không tìm thấy.</li>
 *   <li>Filter kết quả theo {@code status} và {@code keyword} ở tầng Controller
 *       sau khi Service đã tính xong health.</li>
 * </ol>
 */
@Tag(name = "Group Monitoring", description = "Giám sát sức khoẻ nhóm sinh viên theo lớp học")
@RestController
@RequestMapping("/api/classes/{classId}/monitoring")
@RequiredArgsConstructor
public class GroupMonitoringController {

    private final GroupMonitoringService groupMonitoringService;
    private final AcademicClassRepository academicClassRepository;

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/classes/{classId}/monitoring/groups
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách trạng thái sức khoẻ của tất cả nhóm trong một lớp học.
     *
     * <h4>Query Parameters</h4>
     * <ul>
     *   <li>{@code fromDate} – Ngày bắt đầu khoảng commit (yyyy-MM-dd). Tuỳ chọn.</li>
     *   <li>{@code toDate} – Ngày kết thúc khoảng commit (yyyy-MM-dd). Tuỳ chọn.</li>
     *   <li>{@code lastNDays} – Số ngày gần đây (ghi đè fromDate/toDate). Tuỳ chọn.</li>
     *   <li>{@code status} – Lọc theo healthStatus: {@code HEALTHY}, {@code WARNING},
     *       {@code CRITICAL}, {@code CLOSED}. Tuỳ chọn.</li>
     *   <li>{@code keyword} – Tìm kiếm theo tên nhóm (case-insensitive). Tuỳ chọn.</li>
     * </ul>
     *
     * <h4>Response</h4>
     * Danh sách {@link GroupMonitoringDTO} sắp xếp theo mức độ nghiêm trọng
     * (CRITICAL → WARNING → CLOSED → HEALTHY). Trả về {@code []} nếu không có nhóm nào.
     *
     * <h4>Lỗi có thể trả về</h4>
     * <ul>
     *   <li>{@code 401} – Chưa đăng nhập.</li>
     *   <li>{@code 403} – Không có quyền truy cập lớp này.</li>
     *   <li>{@code 404} – classId không tồn tại.</li>
     *   <li>{@code 400} – Tham số ngày không hợp lệ (fromDate > toDate, lastNDays < 1).</li>
     * </ul>
     *
     * @param classId    ID lớp học (path variable)
     * @param fromDate   Ngày bắt đầu (query param, yyyy-MM-dd)
     * @param toDate     Ngày kết thúc (query param, yyyy-MM-dd)
     * @param lastNDays  Số ngày gần đây (query param)
     * @param status     Lọc theo healthStatus (query param)
     * @param keyword    Tìm kiếm theo tên nhóm (query param)
     * @return danh sách {@link GroupMonitoringDTO} đã lọc
     */
    @Operation(
        summary = "Lấy danh sách trạng thái sức khoẻ nhóm theo lớp",
        description = """
            Trả về danh sách monitoring của tất cả nhóm trong một lớp học.
            Hỗ trợ lọc theo healthStatus (HEALTHY/WARNING/CRITICAL/CLOSED) và tìm kiếm
            theo tên nhóm. Kết quả sắp xếp theo mức độ nghiêm trọng giảm dần.
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

            @Parameter(description = "Số ngày gần đây (ghi đè fromDate/toDate, >= 1)")
            @RequestParam(required = false) Integer lastNDays,

            @Parameter(description = "Lọc health status: HEALTHY | WARNING | CRITICAL | CLOSED")
            @RequestParam(required = false) String status,

            @Parameter(description = "Tìm kiếm theo tên nhóm (case-insensitive)")
            @RequestParam(required = false) String keyword
    ) {
        // ── 1. Kiểm tra classId tồn tại ──────────────────────────────────────
        if (!academicClassRepository.existsById(classId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy lớp học với id = " + classId);
        }

        // ── 2. Build filter request & gọi Service ────────────────────────────
        MonitoringFilterRequest filter = MonitoringFilterRequest.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .lastNDays(lastNDays)
                .status(status)
                .keyword(keyword)
                .build();

        List<GroupMonitoringDTO> results = groupMonitoringService.getByClass(classId, filter);

        // ── 3. Áp dụng filter status (post-query) ────────────────────────────
        // Filter status được thực hiện sau khi Service đã classify health
        // vì health là giá trị tính toán động, không lưu trong DB.
        if (status != null && !status.isBlank()) {
            HealthStatus targetStatus = parseHealthStatus(status);
            results = results.stream()
                    .filter(dto -> dto.getHealthStatus() == targetStatus)
                    .collect(Collectors.toList());
        }

        // ── 4. Áp dụng filter keyword (post-query) ──────────────────────────
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            results = results.stream()
                    .filter(dto -> dto.getGroupName() != null
                            && dto.getGroupName().toLowerCase().contains(kw))
                    .collect(Collectors.toList());
        }

        // ── 5. Build response ─────────────────────────────────────────────────
        if (results.isEmpty()) {
            return ResponseEntity.ok(
                    new ApiResponse<>(200, "Không có nhóm nào phù hợp với điều kiện lọc.", List.of()));
        }

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Parse chuỗi status thành {@link HealthStatus} enum.
     * Ném {@code 400 Bad Request} nếu giá trị không hợp lệ.
     *
     * @param status chuỗi từ query param
     * @return {@link HealthStatus} tương ứng
     * @throws ResponseStatusException {@code 400} nếu status không hợp lệ
     */
    private HealthStatus parseHealthStatus(String status) {
        try {
            return HealthStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giá trị status không hợp lệ: '" + status
                    + "'. Chấp nhận: HEALTHY, WARNING, CRITICAL, CLOSED");
        }
    }
}
