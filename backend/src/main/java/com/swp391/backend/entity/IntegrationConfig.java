package com.swp391.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "IntegrationConfig")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "integration_type_id", nullable = false)
    private Integer integrationTypeId;

    // Jira config
    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "project_key")
    private String projectKey;

    @Column(name = "jira_email")
    private String jiraEmail;

    // GitHub config
    @Column(name = "repo_full_name")
    private String repoFullName;

    @Column(name = "token_encrypted")
    private byte[] tokenEncrypted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
