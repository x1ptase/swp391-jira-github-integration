package com.swp391.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangeGroupStatusRequest {
    @NotBlank
    @Pattern(regexp = "^(OPEN|CLOSED)$", message = "Status must be OPEN or CLOSED")
    private String status;
}
