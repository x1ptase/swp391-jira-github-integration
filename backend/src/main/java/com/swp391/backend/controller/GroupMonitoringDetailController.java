package com.swp391.backend.controller;

import com.swp391.backend.dto.monitoring.shared.GroupMonitoringDetailResponse;
import com.swp391.backend.dto.response.ApiResponse;
import com.swp391.backend.service.monitoring.GroupMonitoringDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupMonitoringDetailController {

    private final GroupMonitoringDetailService groupMonitoringDetailService;

    @GetMapping("/{groupId}/monitoring/detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'LECTURER')")
    public ResponseEntity<ApiResponse<GroupMonitoringDetailResponse>> getGroupMonitoringDetail(
            @PathVariable Long groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        groupMonitoringDetailService.getDetail(groupId, fromDate, toDate)));
    }
}