package com.swp391.backend.repository;

import com.swp391.backend.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@org.springframework.stereotype.Repository
public interface RepositoryRepository extends JpaRepository<Repository, Integer> {
    Optional<Repository> findByGroupIdAndFullName(Long groupId, String fullName);
}
