package com.swp391.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "IntegrationType")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_type_id")
    private Integer integrationTypeId;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;
}
