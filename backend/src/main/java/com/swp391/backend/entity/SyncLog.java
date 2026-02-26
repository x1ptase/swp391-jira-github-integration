package com.swp391.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SyncLogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SyncStatus status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "inserted_count")
    private Integer insertedCount;

    @Column(name = "updated_count")
    private Integer updatedCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
