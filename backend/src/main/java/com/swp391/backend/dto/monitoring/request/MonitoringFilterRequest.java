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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringFilterRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    @Min(value = 1, message = "lastNDays phải >= 1")
    private Integer lastNDays;

    private String semesterCode;

    private String keyword;

    @Positive(message = "groupId phải > 0")
    private Long groupId;

    private String status;

    @Min(value = 0, message = "page phải >= 0")
    @Builder.Default
    private int page = 0;

    @Positive(message = "size phải >= 1")
    @Builder.Default
    private int size = 20;
}
