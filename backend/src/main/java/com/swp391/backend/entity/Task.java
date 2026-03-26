package com.swp391.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "Task")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Integer taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id", nullable = false)
    private Requirement requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup studentGroup;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false)
    private TaskStatus status;

    @Column(name = "estimate_hours", precision = 6, scale = 2)
    private BigDecimal estimateHours;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "jira_issue_key", length = 50, unique = true)
    private String jiraIssueKey;

    /**
     * Self-reference FK cho sub-tasks.
     * Story (top-level task) có parentTask = null.
     * Sub-task có parentTask trỏ tới Story tương ứng.
     * Mapping với cột parent_task_id trong DB.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    /** Raw issue type từ Jira (e.g. "STORY", "SUBTASK"). */
    @Column(name = "jira_issue_type", length = 50)
    private String jiraIssueType;

    /**
     * Jira key của issue cha.
     * Story → Epic key; Sub-task → Story key.
     */
    @Column(name = "jira_parent_issue_key", length = 50)
    private String jiraParentIssueKey;

    /** Raw status name từ Jira (e.g. "To Do", "In Progress", "Done"). */
    @Column(name = "jira_status_raw", length = 100)
    private String jiraStatusRaw;

    /** Raw priority name từ Jira (e.g. "High", "Medium"). Nullable. */
    @Column(name = "jira_priority_raw", length = 100)
    private String jiraPriorityRaw;

    /** Jira accountId của assignee. Dùng để map với Users.jira_account_id. */
    @Column(name = "jira_assignee_account_id", length = 255)
    private String jiraAssigneeAccountId;

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
