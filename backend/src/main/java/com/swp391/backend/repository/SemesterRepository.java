package com.swp391.backend.repository;

import com.swp391.backend.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    Optional<Semester> findBySemesterCode(String semesterCode);
}
