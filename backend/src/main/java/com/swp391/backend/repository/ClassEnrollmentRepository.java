package com.swp391.backend.repository;

import com.swp391.backend.entity.ClassEnrollment;
import com.swp391.backend.entity.ClassEnrollmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, ClassEnrollmentId> {

    boolean existsByAcademicClass_ClassIdAndStudent_UserId(Long classId, Long studentId);

    boolean existsByStudent_UserIdAndAcademicClass_ClassId(Long studentId, Long classId);


}