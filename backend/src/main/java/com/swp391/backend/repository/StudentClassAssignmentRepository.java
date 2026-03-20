package com.swp391.backend.repository;

import com.swp391.backend.entity.StudentClassAssignment;
import com.swp391.backend.entity.StudentClassAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentClassAssignmentRepository
        extends JpaRepository<StudentClassAssignment, StudentClassAssignmentId> {

    Optional<StudentClassAssignment> findByStudent_UserId(Long studentId);

    boolean existsByAcademicClass_ClassIdAndStudent_UserId(Long classId, Long studentId);

    boolean existsByStudent_UserId(Long studentId);

    long countByAcademicClass_ClassId(Long classId);

    List<StudentClassAssignment> findByAcademicClass_ClassId(Long classId);

    void deleteByAcademicClass_ClassIdAndStudent_UserId(Long classId, Long studentId);
}