package com.swp391.backend.repository;

import com.swp391.backend.entity.StudentGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
    @Transactional
    void removeStudentGroupByGroupId(Long groupId);

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"academicClass", "academicClass.course", "academicClass.semester"})
    Optional<StudentGroup> findById(@NonNull Long id);

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"academicClass", "academicClass.course", "academicClass.semester"})
    List<StudentGroup> findAll();

    @EntityGraph(attributePaths = {"academicClass", "academicClass.course", "academicClass.semester"})
    List<StudentGroup> findByGroupId(Long groupId);

    boolean existsByAcademicClass_ClassCodeAndGroupIdNot(String classCode, Long groupId);

    boolean existsByAcademicClass_ClassIdAndGroupName(Long classId, String groupName);

    boolean existsByAcademicClass_ClassIdAndGroupNameAndGroupIdNot(Long classId, String groupName, Long groupId);

    @EntityGraph(attributePaths = {"academicClass", "academicClass.course", "academicClass.semester"})
    List<StudentGroup> findByAcademicClass_Course_CourseCode(String courseCode);

    @EntityGraph(attributePaths = {"academicClass", "academicClass.course", "academicClass.semester"})
    List<StudentGroup> findByAcademicClass_Semester_SemesterCode(String semesterCode);

    @EntityGraph(attributePaths = {"academicClass", "academicClass.course", "academicClass.semester"})
    List<StudentGroup> findByAcademicClass_Course_CourseCodeAndAcademicClass_Semester_SemesterCode(String courseCode, String semesterCode);

    @EntityGraph(attributePaths = {"academicClass", "academicClass.course", "academicClass.semester"})
    List<StudentGroup> findByAcademicClass_ClassId(Long classId);
}
