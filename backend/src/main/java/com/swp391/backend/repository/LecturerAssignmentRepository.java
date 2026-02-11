package com.swp391.backend.repository;

import com.swp391.backend.entity.LecturerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LecturerAssignmentRepository extends JpaRepository<LecturerAssignment, Long> {
    List<LecturerAssignment> findByLecturerId(Long lecturerId);
}

