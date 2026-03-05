package com.swp391.backend.repository;

import com.swp391.backend.entity.GitCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GitCommitRepository extends JpaRepository<GitCommit, Integer> {
    Optional<GitCommit> findByRepoIdAndSha(Integer repoId, String sha);
}
