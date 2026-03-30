package com.swp391.backend.service.monitoring;

import com.swp391.backend.config.MonitoringConfig;
import com.swp391.backend.dto.monitoring.request.MonitoringFilterRequest;
import com.swp391.backend.dto.monitoring.shared.MonitoringDateRange;
import com.swp391.backend.exception.MonitoringValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Service chuyên trách resolve và validate khoảng thời gian monitoring.
 *
 * <h4>Quy tắc ưu tiên resolve:</h4>
 * <ol>
 *   <li>Nếu {@code fromDate} và {@code toDate} có giá trị → dùng trực tiếp.</li>
 *   <li>Nếu {@code lastNDays} có giá trị → từ = now - lastNDays, đến = now.</li>
 *   <li>Không có gì → dùng default window (configurable, mặc định 7 ngày).</li>
 * </ol>
 *
 * <h4>Normalization:</h4>
 * <ul>
 *   <li>{@code from} → {@code atStartOfDay()} (00:00:00)</li>
 *   <li>{@code to} → {@code atTime(LocalTime.MAX)} (23:59:59.999999999)</li>
 * </ul>
 *
 * <h4>Timezone assumption:</h4>
 * Dùng {@code LocalDateTime.now()} của JVM. Cần đảm bảo JVM timezone khớp
 * với kỳ vọng của dự án (khuyến nghị: set {@code -Duser.timezone=Asia/Ho_Chi_Minh}).
 */
@Service
@RequiredArgsConstructor
public class MonitoringDateRangeService {

    private final MonitoringConfig monitoringConfig;

    /**
     * Resolve {@link MonitoringDateRange} từ filter request.
     *
     * @param filter filter request từ controller (có thể null, sẽ dùng default)
     * @return khoảng thời gian đã validate và normalize
     * @throws MonitoringValidationException nếu fromDate > toDate hoặc lastNDays < 1
     */
    public MonitoringDateRange resolve(MonitoringFilterRequest filter) {
        if (filter == null) {
            return defaultRange();
        }

        // Priority 1: fromDate + toDate
        if (filter.getFromDate() != null && filter.getToDate() != null) {
            return resolveFromExplicitDates(filter.getFromDate(), filter.getToDate());
        }

        // Priority 2: lastNDays
        if (filter.getLastNDays() != null) {
            return resolveFromLastNDays(filter.getLastNDays());
        }

        // Priority 3: default window
        return defaultRange();
    }

    /**
     * Resolve range từ ngày cụ thể. Validate fromDate <= toDate.
     *
     * @throws MonitoringValidationException nếu fromDate > toDate
     */
    private MonitoringDateRange resolveFromExplicitDates(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new MonitoringValidationException(
                    "fromDate (" + fromDate + ") không thể sau toDate (" + toDate + ")");
        }
        return MonitoringDateRange.builder()
                .from(fromDate.atStartOfDay())
                .to(toDate.atTime(LocalTime.MAX))
                .build();
    }

    /**
     * Resolve range từ lastNDays ngày gần đây. Validate lastNDays >= 1.
     *
     * @throws MonitoringValidationException nếu lastNDays < 1
     */
    private MonitoringDateRange resolveFromLastNDays(int lastNDays) {
        if (lastNDays < 1) {
            throw new MonitoringValidationException("lastNDays phải >= 1, nhận: " + lastNDays);
        }
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(lastNDays);
        return MonitoringDateRange.builder()
                .from(from.atStartOfDay())
                .to(today.atTime(LocalTime.MAX))
                .build();
    }

    /**
     * Tạo default range dựa trên config ({@code defaultMonitoringWindowDays}).
     */
    private MonitoringDateRange defaultRange() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(monitoringConfig.getDefaultMonitoringWindowDays());
        return MonitoringDateRange.builder()
                .from(from.atStartOfDay())
                .to(today.atTime(LocalTime.MAX))
                .build();
    }

    /**
     * Tạo range từ LocalDateTime cụ thể (dùng cho testing hoặc advanced usage).
     *
     * @param from mốc bắt đầu (inclusive)
     * @param to   mốc kết thúc (inclusive)
     * @throws MonitoringValidationException nếu from > to
     */
    public MonitoringDateRange of(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new MonitoringValidationException(
                    "from (" + from + ") không thể sau to (" + to + ")");
        }
        return MonitoringDateRange.builder().from(from).to(to).build();
    }
}
