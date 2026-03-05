package com.swp391.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "GitCommit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "commit_id")
    private Integer commitId;

    @Column(name = "repo_id", nullable = false)
    private Integer repoId;

    @Column(name = "sha", nullable = false, length = 64)
    private String sha;

    @Column(name = "author_user_id")
    private Long authorUserId;

    @Column(name = "author_name", length = 120)
    private String authorName;

    @Column(name = "author_email", length = 120)
    private String authorEmail;

    @Column(name = "author_login", length = 100)
    private String authorLogin;

    @Column(name = "commit_date", nullable = false)
    private LocalDateTime commitDate;

    @Lob
    @Column(name = "message", columnDefinition = "NVARCHAR(MAX)")
    private String message;

    @Column(name = "additions")
    private Integer additions;

    @Column(name = "deletions")
    private Integer deletions;

    @Column(name = "files_changed")
    private Integer filesChanged;
}
