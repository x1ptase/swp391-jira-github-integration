package com.swp391.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollmentId implements Serializable {

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "student_id")
    private Long studentId;
}