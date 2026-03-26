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
    @Column(name = "class_id", nullable = false)
    private Long classId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "class_id")
    private AcademicClass academicClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    public LecturerAssignment() {

    }

}
