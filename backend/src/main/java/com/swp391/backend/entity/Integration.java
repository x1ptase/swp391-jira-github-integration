package com.swp391.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Integration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Integration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "encrypted_token")
    private String encryptedToken;
}
