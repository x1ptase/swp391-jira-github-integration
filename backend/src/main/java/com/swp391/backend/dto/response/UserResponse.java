package com.swp391.backend.dto.response;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class UserResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String githubUsername;
    private String jiraAccountId;
    private String roleCode;
    private LocalDateTime createdAt;

    public UserResponse() {
    }

}
