package com.swp391.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TaskStatus")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "code", unique = true, nullable = false, length = 30)
    private String code;
}
