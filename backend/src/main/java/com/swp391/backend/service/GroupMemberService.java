package com.swp391.backend.service;

import com.swp391.backend.dto.response.GroupMemberResponse;

import java.util.List;

public interface GroupMemberService {
    void addMember(Long groupId, Long studentId);
    void removeMember(Long groupId, Long studentId);
    void setLeader(Long groupId, Long studentId);
    List<GroupMemberResponse> listMembers(Long groupId);
}