package com.swp391.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateGroupRequest {
    @NotNull
    private Long classId;

    @NotBlank
    private String groupName;
}
