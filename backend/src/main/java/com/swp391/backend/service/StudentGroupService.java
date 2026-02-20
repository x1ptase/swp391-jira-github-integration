package com.swp391.backend.service;

import com.swp391.backend.dto.request.CreateGroupRequest;
import com.swp391.backend.dto.response.StudentGroupResponse;
import com.swp391.backend.entity.StudentGroup;

import java.util.List;

public interface StudentGroupService {
    StudentGroupResponse addStudentGroup(CreateGroupRequest request);

    StudentGroupResponse updateStudentGroup(StudentGroup studentGroup, Long id);

    StudentGroupResponse deleteStudentGroup(Long studentGroupId);

    List<StudentGroupResponse> listStudentGroups(String courseCode, String semester);
}
