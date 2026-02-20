package com.swp391.backend.controller;

import com.swp391.backend.common.ApiResponse;
import com.swp391.backend.dto.request.AddMemberRequest;
import com.swp391.backend.dto.request.SetLeaderRequest;
import com.swp391.backend.dto.response.GroupMemberResponse;
import com.swp391.backend.service.GroupMemberService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    public GroupMemberController(GroupMemberService groupMemberService) {
        this.groupMemberService = groupMemberService;
    }

    @PostMapping("/{groupId}/members")
    @PreAuthorize("@securityService.hasAccessToGroup(#groupId)")
    public ApiResponse<Object> addMember(@PathVariable Long groupId,
            @Valid @RequestBody AddMemberRequest req) {
        groupMemberService.addMember(groupId, req.getUserId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @PreAuthorize("@securityService.hasAccessToGroup(#groupId)")
    public ApiResponse<Object> removeMember(@PathVariable Long groupId,
            @PathVariable Long userId) {
        groupMemberService.removeMember(groupId, userId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{groupId}/leader")
    @PreAuthorize("@securityService.hasAccessToGroup(#groupId)")
    public ApiResponse<Object> setLeader(@PathVariable Long groupId,
            @Valid @RequestBody SetLeaderRequest req) {
        groupMemberService.setLeader(groupId, req.getUserId());
        return ApiResponse.success(null);
    }

    @GetMapping("/{groupId}/members")
    @PreAuthorize("@securityService.hasAccessToGroup(#groupId)")
    public ApiResponse<List<GroupMemberResponse>> list(@PathVariable Long groupId) {
        return ApiResponse.success(groupMemberService.listMembers(groupId));
    }
}