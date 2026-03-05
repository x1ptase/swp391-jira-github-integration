package com.swp391.backend.common;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    /**
     * Trả về chuỗi thời gian định dạng ISO 8601 của 'N ngày trước'
     * Ví dụ: 2024-03-01T00:00:00Z
     *
     * @param days số ngày trước
     * @return chuỗi thời gian định dạng ISO 8601
     */
    public static String getIsoDateLastNDays(int days) {
        return OffsetDateTime.now(ZoneOffset.UTC)
                .minusDays(days)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .format(DateTimeFormatter.ISO_INSTANT);
    }
}
