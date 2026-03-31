package com.swp391.backend.controller;

import com.swp391.backend.dto.monitoring.shared.LecturerClassMonitoringResponse;
import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.service.monitoring.LecturerClassMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/lecturer/monitoring")
@RequiredArgsConstructor
public class LecturerClassMonitoringController {

    private final LecturerClassMonitoringService lecturerClassMonitoringService;

    @GetMapping("/classes")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<ApiResponse<List<LecturerClassMonitoringResponse>>> getMyClassesMonitoring(
            @RequestParam(required = false) String semesterCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) Integer lastNDays,
            @RequestParam(required = false) String keyword) {

        // lastNDays override fromDate/toDate nếu có
        if (lastNDays != null && lastNDays > 0) {
            toDate   = LocalDateTime.now();
            fromDate = toDate.minusDays(lastNDays);
        } else {
            if (fromDate == null) fromDate = LocalDateTime.now().minusDays(7);
            if (toDate == null)   toDate   = LocalDateTime.now();
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        lecturerClassMonitoringService.getMyClassesMonitoring(
                                semesterCode, fromDate, toDate, keyword)));
    }
}