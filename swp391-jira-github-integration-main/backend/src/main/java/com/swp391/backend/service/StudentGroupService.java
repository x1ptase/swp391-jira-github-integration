package com.swp391.backend.service;

import com.swp391.backend.entity.StudentGroup;

import java.util.List;

public interface StudentGroupService  {
    StudentGroup addStudentGroup(StudentGroup studentGroup);
    StudentGroup updateStudentGroup(StudentGroup studentGroup , Long id);
    StudentGroup deleteStudentGroup(Long studentGroupId);
    List<StudentGroup> listStudentGroups(String courseCode, String semester);

}
