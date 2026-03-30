package com.swp391.backend.dto.monitoring.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Request/filter dùng chung cho tất cả các monitoring API.
 * <p>
 * <b>Quy tắc resolve ngày tháng (theo thứ tự ưu tiên):</b>
 * <ol>
 *   <li>Nếu {@code fromDate} và {@code toDate} đều có giá trị → dùng trực tiếp.</li>
 *   <li>Nếu {@code lastNDays} có giá trị → {@code fromDate = now - lastNDays}, {@code toDate = now}.</li>
 *   <li>Nếu không có gì → dùng default window từ {@link com.swp391.backend.config.MonitoringConfig#getDefaultMonitoringWindowDays()}.</li>
 * </ol>
 *
 * <p><b>Validation:</b>
 * <ul>
 *   <li>{@code fromDate} phải &le; {@code toDate} (kiểm tra tại service layer).</li>
 *   <li>{@code lastNDays} phải &ge; 1 nếu được cung cấp.</li>
 *   <li>{@code page} &ge; 0 và {@code size} &ge; 1.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringFilterRequest {

    /**
     * Ngày bắt đầu khoảng monitoring (inclusive).
     * Format: {@code yyyy-MM-dd}.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    /**
     * Ngày kết thúc khoảng monitoring (inclusive).
     * Format: {@code yyyy-MM-dd}.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    /**
     * Số ngày gần đây để tính monitoring window.
     * Nếu set, sẽ override fromDate/toDate.
     * Phải &ge; 1.
     */
    @Min(value = 1, message = "lastNDays phải >= 1")
    private Integer lastNDays;

    /**
     * Lọc theo mã học kỳ (ví dụ: "SU25").
     * Tuỳ chọn.
     */
    private String semesterCode;

    /**
     * Từ khoá tìm kiếm (tên nhóm, tên lớp, tên sinh viên...).
     * Tuỳ chọn, tìm kiếm case-insensitive.
     */
    private String keyword;

    /**
     * Lọc theo một nhóm cụ thể.
     * Tuỳ chọn.
     */
    @Positive(message = "groupId phải > 0")
    private Long groupId;

    /**
     * Lọc theo trạng thái health (HEALTHY / WARNING / CRITICAL).
     * Tuỳ chọn.
     */
    private String status;

    /**
     * Số trang (0-indexed).
     * Mặc định: 0.
     */
    @Min(value = 0, message = "page phải >= 0")
    @Builder.Default
    private int page = 0;

    /**
     * Kích thước trang.
     * Mặc định: 20.
     */
    @Positive(message = "size phải >= 1")
    @Builder.Default
    private int size = 20;
}
