package com.swp391.backend.service.impl;

import com.swp391.backend.dto.request.CreateGroupRequest;
import com.swp391.backend.dto.response.StudentGroupResponse;
import com.swp391.backend.entity.LecturerAssignment;
import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.entity.User;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.LecturerAssignmentRepository;
import com.swp391.backend.repository.StudentGroupRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.service.LecturerAssignmentService;
import com.swp391.backend.service.StudentGroupService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentGroupServiceImpl implements StudentGroupService {
    private final StudentGroupRepository studentGroupRepository;
    private final LecturerAssignmentService lecturerAssignmentService;
    private final UserRepository userRepository;
    private final LecturerAssignmentRepository lecturerAssignmentRepository;

    public StudentGroupServiceImpl(StudentGroupRepository studentGroupRepository,
            LecturerAssignmentService lecturerAssignmentService,
            UserRepository userRepository,
            LecturerAssignmentRepository lecturerAssignmentRepository) {
        this.studentGroupRepository = studentGroupRepository;
        this.lecturerAssignmentService = lecturerAssignmentService;
        this.userRepository = userRepository;
        this.lecturerAssignmentRepository = lecturerAssignmentRepository;
    }

    @Override
    @Transactional
    public StudentGroupResponse addStudentGroup(CreateGroupRequest request) {

        StudentGroup studentGroup = new StudentGroup();
        studentGroup.setClassCode(request.getClassCode());
        studentGroup.setGroupName(request.getGroupName());
        studentGroup.setCourseCode(request.getCourseCode());
        studentGroup.setSemester(request.getSemester());

        StudentGroup savedGroup = studentGroupRepository.save(studentGroup);

        Long fid = request.getLecturerId();
        if (fid == null && StringUtils.hasText(request.getLecturerName())) {
            var users = userRepository.searchWithRole(request.getLecturerName().trim(), "LECTURER",
                    PageRequest.of(0, 1));
            if (!users.isEmpty()) {
                fid = users.getContent().get(0).getUserId();
            }
        }

        if (fid != null) {
            lecturerAssignmentService.assignLecturer(savedGroup.getGroupId(), fid);
        }

        return mapToResponse(savedGroup);
    }

    @Override
    public StudentGroupResponse updateStudentGroup(StudentGroup studentGroup, Long id) {
        StudentGroup existing = studentGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException("StudentGroup not found: " + id, 404));

        if (studentGroupRepository.existsByClassCodeAndGroupIdNot(studentGroup.getClassCode(), id)) {
            throw new BusinessException("StudentGroup groupCode already exists: " + studentGroup.getClassCode(), 409);
        }

        existing.setGroupName(studentGroup.getGroupName());
        existing.setCourseCode(studentGroup.getCourseCode());
        existing.setSemester(studentGroup.getSemester());

        return mapToResponse(studentGroupRepository.save(existing));
    }

    @Override
    @Transactional
    public StudentGroupResponse deleteStudentGroup(Long studentGroupId) {
        StudentGroup existing = studentGroupRepository.findById(studentGroupId)
                .orElseThrow(() -> new BusinessException("StudentGroup not found: " + studentGroupId, 404));
        studentGroupRepository.delete(existing);
        return mapToResponse(existing);
    }

    @Override
    public List<StudentGroupResponse> listStudentGroups(String courseCode, String semester) {
        String normalizedCourseCode = StringUtils.hasText(courseCode) ? courseCode.trim() : null;
        String normalizedSemester = StringUtils.hasText(semester) ? semester.trim() : null;

        List<StudentGroup> groups;
        if (normalizedCourseCode != null && normalizedSemester != null) {
            groups = studentGroupRepository.findByCourseCodeAndSemester(normalizedCourseCode, normalizedSemester);
        } else if (normalizedCourseCode != null) {
            groups = studentGroupRepository.findByCourseCode(normalizedCourseCode);
        } else if (normalizedSemester != null) {
            groups = studentGroupRepository.findBySemester(normalizedSemester);
        } else {
            groups = studentGroupRepository.findAll();
        }

        return groups.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private StudentGroupResponse mapToResponse(StudentGroup group) {
        StudentGroupResponse resp = StudentGroupResponse.builder()
                .groupId(group.getGroupId())
                .classCode(group.getClassCode())
                .groupName(group.getGroupName())
                .courseCode(group.getCourseCode())
                .semester(group.getSemester())
                .createdAt(group.getCreatedAt())
                .build();

        Optional<LecturerAssignment> assignOpt = lecturerAssignmentRepository.findById(group.getGroupId());
        if (assignOpt.isPresent()) {
            Long lecturerId = assignOpt.get().getLecturerId();
            resp.setLecturerId(lecturerId);
            userRepository.findById(lecturerId).ifPresent(u -> resp.setLecturerName(u.getFullName()));
        }

        return resp;
    }
}