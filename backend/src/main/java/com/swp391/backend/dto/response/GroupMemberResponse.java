package com.swp391.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupMemberResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String memberRole;
    private String email;
    public GroupMemberResponse() {}
}