package com.swp391.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LecturerAssignment")
public class LecturerAssignment {

    @Id
    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "lecturer_id", nullable = false)
    private Long lecturerId;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    public LecturerAssignment() {

    }

    public Long getGroupId() {
        return groupId;
    }
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getLecturerId() {
        return lecturerId;
    }
    public void setLecturerId(Long lecturerId) {
        this.lecturerId = lecturerId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}

