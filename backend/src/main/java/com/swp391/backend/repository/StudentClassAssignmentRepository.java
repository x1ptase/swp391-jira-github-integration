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

    @org.springframework.data.jpa.repository.Query("SELECT sca.student FROM StudentClassAssignment sca " +
            "WHERE sca.academicClass.classId = :classId " +
            "AND (:search IS NULL " +
            "     OR LOWER(sca.student.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(sca.student.studentCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:hasGroup IS NULL " +
            "     OR (:hasGroup = true AND EXISTS (SELECT 1 FROM GroupMember gm WHERE gm.user.userId = sca.student.userId AND gm.group.academicClass.classId = :classId)) " +
            "     OR (:hasGroup = false AND NOT EXISTS (SELECT 1 FROM GroupMember gm WHERE gm.user.userId = sca.student.userId AND gm.group.academicClass.classId = :classId)))")
    org.springframework.data.domain.Page<com.swp391.backend.entity.User> searchStudentsInClass(
            @org.springframework.data.repository.query.Param("classId") Long classId,
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("hasGroup") Boolean hasGroup,
            org.springframework.data.domain.Pageable pageable);
}