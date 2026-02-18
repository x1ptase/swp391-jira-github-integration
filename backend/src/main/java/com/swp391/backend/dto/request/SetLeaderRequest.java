package com.swp391.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SetLeaderRequest {
    @NotNull
    private Long userId;

    public SetLeaderRequest() {}

}