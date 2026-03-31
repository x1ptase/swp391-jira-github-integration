package com.swp391.backend.controller;

import com.swp391.backend.dto.monitoring.shared.ClassMonitoringSummaryResponse;
import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.service.monitoring.ClassMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassMonitoringController {

    private final ClassMonitoringService classMonitoringService;

    @GetMapping("/{classId}/monitoring/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    public ResponseEntity<ApiResponse<ClassMonitoringSummaryResponse>> getClassMonitoringSummary(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        classMonitoringService.getSummary(classId, fromDate, toDate)));
    }
}