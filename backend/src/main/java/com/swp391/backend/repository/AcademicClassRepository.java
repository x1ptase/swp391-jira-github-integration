package com.swp391.backend.repository;

import com.swp391.backend.entity.AcademicClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicClassRepository extends JpaRepository<AcademicClass, Long> {
    Optional<AcademicClass> findByClassCode(String classCode);
    List<AcademicClass> findByCourse_CourseCodeAndSemester_SemesterCode(String courseCode, String semesterCode);
}
