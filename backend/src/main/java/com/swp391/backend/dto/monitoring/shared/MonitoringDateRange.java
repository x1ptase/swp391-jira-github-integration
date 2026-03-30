package com.swp391.backend.dto.monitoring.shared;

import lombok.Getter;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Khoảng thời gian đã được resolve (bắt đầu + kết thúc).
 * <p>
 * Được tạo ra bởi {@link com.swp391.backend.service.monitoring.MonitoringDateRangeService}
 * từ {@link MonitoringFilterRequest} và được truyền vào các repository query.
 *
 * <p><b>Timezone:</b> Sử dụng LocalDateTime của server.
 * {@code from} luôn là đầu ngày (00:00:00), {@code to} là cuối ngày (23:59:59.999999999).
 */
@Getter
@Builder
public class MonitoringDateRange {

    /**
     * Mốc bắt đầu (inclusive), đã được normalize về 00:00:00.
     */
    private final LocalDateTime from;

    /**
     * Mốc kết thúc (inclusive), đã được normalize về 23:59:59.999999999.
     */
    private final LocalDateTime to;
}
