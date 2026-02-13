package com.swp391.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
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

}

