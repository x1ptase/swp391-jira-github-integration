package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentClassDetailsResponse {
    private Long userId;
    private String studentCode; // MSSV
    private String username;
    private String fullName;
    private String email;
    private Long groupId;
    private String groupName;
    private String memberRole;
}
