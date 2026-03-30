package com.swp391.backend.service.monitoring;

import com.swp391.backend.config.MonitoringConfig;
import com.swp391.backend.dto.monitoring.request.MonitoringFilterRequest;
import com.swp391.backend.dto.monitoring.shared.MonitoringDateRange;
import com.swp391.backend.exception.MonitoringValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests cho {@link MonitoringDateRangeService}.
 */
@DisplayName("MonitoringDateRangeService Tests")
class MonitoringDateRangeServiceTest {

    private MonitoringDateRangeService dateRangeService;
    private MonitoringConfig config;

    @BeforeEach
    void setUp() {
        config = new MonitoringConfig(); // defaultMonitoringWindowDays = 7
        dateRangeService = new MonitoringDateRangeService(config);
    }

    @Nested
    @DisplayName("Null filter → default window")
    class DefaultWindowTests {

        @Test
        @DisplayName("null filter → uses defaultMonitoringWindowDays (7)")
        void nullFilter_usesDefault() {
            MonitoringDateRange range = dateRangeService.resolve(null);

            LocalDate expectedFrom = LocalDate.now().minusDays(7);
            assertThat(range.getFrom().toLocalDate()).isEqualTo(expectedFrom);
            assertThat(range.getTo().toLocalDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("empty filter (no fields set) → uses default")
        void emptyFilter_usesDefault() {
            MonitoringFilterRequest filter = MonitoringFilterRequest.builder().build();
            MonitoringDateRange range = dateRangeService.resolve(filter);

            assertThat(range.getFrom().toLocalDate()).isEqualTo(LocalDate.now().minusDays(7));
            assertThat(range.getTo().toLocalDate()).isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("Explicit fromDate/toDate")
    class ExplicitDateTests {

        @Test
        @DisplayName("Valid fromDate + toDate → normalized correctly")
        void explicitDates_normalized() {
            LocalDate from = LocalDate.of(2026, 3, 1);
            LocalDate to = LocalDate.of(2026, 3, 7);

            MonitoringFilterRequest filter = MonitoringFilterRequest.builder()
                    .fromDate(from)
                    .toDate(to)
                    .build();
            MonitoringDateRange range = dateRangeService.resolve(filter);

            assertThat(range.getFrom()).isEqualTo(from.atStartOfDay());
            assertThat(range.getTo()).isEqualTo(to.atTime(LocalTime.MAX));
        }

        @Test
        @DisplayName("fromDate == toDate → single day range (valid)")
        void sameDay_valid() {
            LocalDate day = LocalDate.of(2026, 3, 15);
            MonitoringFilterRequest filter = MonitoringFilterRequest.builder()
                    .fromDate(day)
                    .toDate(day)
                    .build();
            MonitoringDateRange range = dateRangeService.resolve(filter);

            assertThat(range.getFrom().toLocalDate()).isEqualTo(day);
            assertThat(range.getTo().toLocalDate()).isEqualTo(day);
        }

        @Test
        @DisplayName("fromDate > toDate → throws MonitoringValidationException")
        void invalidRange_throwsException() {
            MonitoringFilterRequest filter = MonitoringFilterRequest.builder()
                    .fromDate(LocalDate.of(2026, 3, 10))
                    .toDate(LocalDate.of(2026, 3, 1))
                    .build();

            assertThatThrownBy(() -> dateRangeService.resolve(filter))
                    .isInstanceOf(MonitoringValidationException.class)
                    .hasMessageContaining("fromDate");
        }
    }

    @Nested
    @DisplayName("lastNDays")
    class LastNDaysTests {

        @Test
        @DisplayName("lastNDays=7 → from = today-7, to = today")
        void lastNDays7_correctRange() {
            MonitoringFilterRequest filter = MonitoringFilterRequest.builder()
                    .lastNDays(7)
                    .build();
            MonitoringDateRange range = dateRangeService.resolve(filter);

            assertThat(range.getFrom().toLocalDate()).isEqualTo(LocalDate.now().minusDays(7));
            assertThat(range.getTo().toLocalDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("lastNDays=1 → from = yesterday, to = today")
        void lastNDays1_correctRange() {
            MonitoringFilterRequest filter = MonitoringFilterRequest.builder()
                    .lastNDays(1)
                    .build();
            MonitoringDateRange range = dateRangeService.resolve(filter);

            assertThat(range.getFrom().toLocalDate()).isEqualTo(LocalDate.now().minusDays(1));
        }

        @Test
        @DisplayName("fromDate+toDate provided with lastNDays → explicit dates take priority")
        void explicitDatesOverrideLastNDays() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 1, 14);

            MonitoringFilterRequest filter = MonitoringFilterRequest.builder()
                    .fromDate(from)
                    .toDate(to)
                    .lastNDays(30) // should be ignored
                    .build();
            MonitoringDateRange range = dateRangeService.resolve(filter);

            // Priority 1: explicit dates win
            assertThat(range.getFrom().toLocalDate()).isEqualTo(from);
            assertThat(range.getTo().toLocalDate()).isEqualTo(to);
        }
    }

    @Nested
    @DisplayName("Boundary normalization")
    class BoundaryTests {

        @Test
        @DisplayName("from is normalized to 00:00:00")
        void fromNormalizedToStartOfDay() {
            MonitoringFilterRequest filter = MonitoringFilterRequest.builder()
                    .fromDate(LocalDate.of(2026, 3, 1))
                    .toDate(LocalDate.of(2026, 3, 7))
                    .build();
            MonitoringDateRange range = dateRangeService.resolve(filter);

            assertThat(range.getFrom().toLocalTime()).isEqualTo(LocalTime.MIDNIGHT);
        }

        @Test
        @DisplayName("to is normalized to 23:59:59.999999999")
        void toNormalizedToEndOfDay() {
            MonitoringFilterRequest filter = MonitoringFilterRequest.builder()
                    .fromDate(LocalDate.of(2026, 3, 1))
                    .toDate(LocalDate.of(2026, 3, 7))
                    .build();
            MonitoringDateRange range = dateRangeService.resolve(filter);

            assertThat(range.getTo().toLocalTime()).isEqualTo(LocalTime.MAX);
        }
    }

    @Nested
    @DisplayName("of() factory method")
    class OfMethodTests {

        @Test
        @DisplayName("of() with valid datetime range → returns correctly")
        void ofValid_returnsCorrectly() {
            LocalDateTime from = LocalDateTime.of(2026, 3, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2026, 3, 7, 23, 59, 59);

            MonitoringDateRange range = dateRangeService.of(from, to);

            assertThat(range.getFrom()).isEqualTo(from);
            assertThat(range.getTo()).isEqualTo(to);
        }

        @Test
        @DisplayName("of() with from > to → throws")
        void ofInvalid_throws() {
            LocalDateTime from = LocalDateTime.of(2026, 3, 10, 0, 0);
            LocalDateTime to = LocalDateTime.of(2026, 3, 1, 0, 0);

            assertThatThrownBy(() -> dateRangeService.of(from, to))
                    .isInstanceOf(MonitoringValidationException.class);
        }
    }
}
