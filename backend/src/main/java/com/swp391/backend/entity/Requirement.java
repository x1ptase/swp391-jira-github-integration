package com.swp391.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Requirement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Requirement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "requirement_id")
    private Integer requirementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup studentGroup;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @ManyToOne
    @JoinColumn(name = "priority_id", nullable = false)
    private Priority priority;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private RequirementStatus status;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "jira_issue_key", length = 50, unique = true)
    private String jiraIssueKey;

    /** Raw issue type từ Jira (e.g. "EPIC"). */
    @Column(name = "jira_issue_type", length = 50)
    private String jiraIssueType;

    /** Raw status name từ Jira (e.g. "In Progress", "Done"). */
    @Column(name = "jira_status_raw", length = 100)
    private String jiraStatusRaw;

    /** Raw priority name từ Jira (e.g. "High", "Medium"). Nullable. */
    @Column(name = "jira_priority_raw", length = 50)
    private String jiraPriorityRaw;

    /** Timestamp updated cuối từ Jira. Nullable. */
    @Column(name = "jira_updated_at")
    private LocalDateTime jiraUpdatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
