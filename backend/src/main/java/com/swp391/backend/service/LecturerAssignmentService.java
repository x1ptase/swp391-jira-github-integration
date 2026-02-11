package com.swp391.backend.service;

import com.swp391.backend.entity.StudentGroup;

import java.util.List;

public interface LecturerAssignmentService {
    void assignLecturer(Long groupId, Long lecturerId);
    List<StudentGroup> getAssignedGroupsForCurrentLecturer();
}
