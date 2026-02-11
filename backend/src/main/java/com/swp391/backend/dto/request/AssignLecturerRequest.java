package com.swp391.backend.dto.request;

import jakarta.validation.constraints.NotNull;

public class AssignLecturerRequest {

    @NotNull
    private Long lecturerId;

    public AssignLecturerRequest() {

    }

    public Long getLecturerId() {
        return lecturerId;
    }
    public void setLecturerId(Long lecturerId) {
        this.lecturerId = lecturerId;
    }
}
