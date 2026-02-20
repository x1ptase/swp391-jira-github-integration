package com.swp391.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateGroupRequest {
    @NotBlank
    private String groupCode;

    @NotBlank
    private String groupName;

    @NotBlank
    private String courseCode;

    @NotBlank
    private String semester;

    private Long lecturerId;

    private String lecturerName;
}
