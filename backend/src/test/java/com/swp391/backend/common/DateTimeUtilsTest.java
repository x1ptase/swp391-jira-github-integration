package com.swp391.backend.common;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilsTest {

    @Test
    void testGetIsoDateLastNDays() {
        int days = 7;
        String result = DateTimeUtils.getIsoDateLastNDays(days);

        // Kiểm tra xem có đúng định dạng ISO 8601 (Instant format thường kết thúc bằng
        // Z)
        assertNotNull(result);
        assertTrue(result.endsWith("Z"));

        // Kiểm tra xem giá trị có khớp với 7 ngày trước lúc 00:00:00 không
        OffsetDateTime expectedDate = OffsetDateTime.now(ZoneOffset.UTC)
                .minusDays(days)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        String expected = expectedDate.format(DateTimeFormatter.ISO_INSTANT);

        assertEquals(expected, result);
    }
}
