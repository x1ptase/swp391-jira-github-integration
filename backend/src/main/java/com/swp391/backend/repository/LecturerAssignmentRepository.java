package com.swp391.backend.repository;

import com.swp391.backend.entity.LecturerAssignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LecturerAssignmentRepository extends JpaRepository<LecturerAssignment, Long> {
    List<LecturerAssignment> findByLecturer_UserId(Long lecturerId);
    boolean existsByClassIdAndLecturer_UserId(Long classId, Long lecturerId);
    boolean existsByClassId(Long classId);
    Optional<LecturerAssignment> findByClassId(Long classId);
}
