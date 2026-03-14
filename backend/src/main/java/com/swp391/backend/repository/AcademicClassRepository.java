package com.swp391.backend.repository;

import com.swp391.backend.entity.AcademicClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicClassRepository extends JpaRepository<AcademicClass, Long> {
    Optional<AcademicClass> findByClassCode(String classCode);
    List<AcademicClass> findByCourse_CourseCodeAndSemester_SemesterCode(String courseCode, String semesterCode);

    @Query("SELECT c FROM AcademicClass c " +
            "WHERE (:kw IS NULL OR :kw = '' OR LOWER(c.classCode) LIKE LOWER(CONCAT('%', :kw, '%'))) " +
            "AND (:courseCode IS NULL OR :courseCode = '' OR LOWER(c.course.courseCode) = LOWER(:courseCode)) " +
            "AND (:semesterCode IS NULL OR :semesterCode = '' OR LOWER(c.semester.semesterCode) = LOWER(:semesterCode))")
    Page<AcademicClass> searchClasses(@Param("kw") String keyword,
                                      @Param("courseCode") String courseCode,
                                      @Param("semesterCode") String semesterCode,
                                      Pageable pageable);
}
