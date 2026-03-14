package com.swp391.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "Semester")
@Data
@NoArgsConstructor
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "semester_id", nullable = false, updatable = false)
    private Long semesterId;

    @Column(name = "semester_code", nullable = false, length = 30)
    private String semesterCode;

    @Column(name = "semester_name", nullable = false, length = 100)
    private String semesterName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
