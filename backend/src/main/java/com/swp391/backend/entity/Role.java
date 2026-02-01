package com.swp391.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Role")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "role_code", unique = true, nullable = false, length = 30)
    private String roleCode;
}
