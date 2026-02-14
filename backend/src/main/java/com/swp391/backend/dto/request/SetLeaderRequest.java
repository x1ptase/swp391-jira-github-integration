package com.swp391.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public class SetLeaderRequest {
    @NotNull
    private Long userId;

    public SetLeaderRequest() {}

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}