package com.swp391.backend.repository;

import com.swp391.backend.entity.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
    @Transactional
    void removeStudentGroupByGroupId(Long groupId);

    List<StudentGroup> findByGroupId(Long groupId);

    boolean existsByGroupCode(String groupCode);

    boolean existsByGroupCodeAndGroupIdNot(String groupCode, Long groupId);

    List<StudentGroup> findByCourseCode(String courseCode);

    List<StudentGroup> findBySemester(String semester);

    List<StudentGroup> findByCourseCodeAndSemester(String courseCode, String semester);
}
