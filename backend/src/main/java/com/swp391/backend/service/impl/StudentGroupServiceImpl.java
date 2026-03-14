package com.swp391.backend.service.impl;

import com.swp391.backend.dto.request.CreateGroupRequest;
import com.swp391.backend.dto.response.StudentGroupResponse;
import com.swp391.backend.entity.AcademicClass;
import com.swp391.backend.entity.GroupMember;
import com.swp391.backend.entity.GroupMemberId;
import com.swp391.backend.entity.LecturerAssignment;
import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.AcademicClassRepository;
import com.swp391.backend.repository.GroupMemberRepository;
import com.swp391.backend.repository.LecturerAssignmentRepository;
import com.swp391.backend.repository.StudentGroupRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.security.SecurityService;
import com.swp391.backend.service.StudentGroupService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentGroupServiceImpl implements StudentGroupService {
    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;
    private final LecturerAssignmentRepository lecturerAssignmentRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final AcademicClassRepository academicClassRepository;
    private final SecurityService securityService;

    public StudentGroupServiceImpl(StudentGroupRepository studentGroupRepository,
                                   UserRepository userRepository,
                                   LecturerAssignmentRepository lecturerAssignmentRepository,
                                   GroupMemberRepository groupMemberRepository,
                                   AcademicClassRepository academicClassRepository,
                                   SecurityService securityService) {
        this.studentGroupRepository = studentGroupRepository;
        this.userRepository = userRepository;
        this.lecturerAssignmentRepository = lecturerAssignmentRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.academicClassRepository = academicClassRepository;
        this.securityService = securityService;
    }

    @Override
    @Transactional
    public StudentGroupResponse addStudentGroup(CreateGroupRequest request) {

        AcademicClass clazz = academicClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new BusinessException("AcademicClass not found: " + request.getClassId(), 404));

        if (studentGroupRepository.existsByAcademicClass_ClassIdAndGroupName(clazz.getClassId(), request.getGroupName())) {
            throw new BusinessException("StudentGroup groupName already exists in this class: " + request.getGroupName(), 409);
        }

        StudentGroup studentGroup = new StudentGroup();
        studentGroup.setAcademicClass(clazz);
        studentGroup.setGroupName(request.getGroupName());

        StudentGroup savedGroup = studentGroupRepository.save(studentGroup);

        return mapToResponse(savedGroup);
    }

    @Override
    @Transactional
    public StudentGroupResponse updateStudentGroup(StudentGroup studentGroup, Long id) {
        StudentGroup existing = studentGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException("StudentGroup not found: " + id, 404));

        if (studentGroupRepository.existsByAcademicClass_ClassIdAndGroupNameAndGroupIdNot(existing.getAcademicClass().getClassId(), studentGroup.getGroupName(), id)) {
            throw new BusinessException("StudentGroup groupName already exists in this class: " + studentGroup.getGroupName(), 409);
        }

        existing.setGroupName(studentGroup.getGroupName());

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
    public List<StudentGroupResponse> listStudentGroups(String courseCode, String semesterCode) {
        String normalizedCourseCode = StringUtils.hasText(courseCode) ? courseCode.trim() : null;
        String normalizedSemester = StringUtils.hasText(semesterCode) ? semesterCode.trim() : null;

        List<StudentGroup> groups;
        if (normalizedCourseCode != null && normalizedSemester != null) {
            groups = studentGroupRepository.findByAcademicClass_Course_CourseCodeAndAcademicClass_Semester_SemesterCode(normalizedCourseCode, normalizedSemester);
        } else if (normalizedCourseCode != null) {
            groups = studentGroupRepository.findByAcademicClass_Course_CourseCode(normalizedCourseCode);
        } else if (normalizedSemester != null) {
            groups = studentGroupRepository.findByAcademicClass_Semester_SemesterCode(normalizedSemester);
        } else {
            groups = studentGroupRepository.findAll();
        }

        return groups.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public StudentGroupResponse getMyGroup() {
        Long currentUserId = securityService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("Unauthorized", 401);
        }

        GroupMember membership = groupMemberRepository.findByUser_UserId(currentUserId)
                .orElseThrow(() -> new BusinessException("Current user does not belong to any group", 404));

        return mapToResponse(membership.getGroup());
    }

    private StudentGroupResponse mapToResponse(StudentGroup group) {
        StudentGroupResponse resp = StudentGroupResponse.builder()
                .groupId(group.getGroupId())
                .classId(group.getAcademicClass().getClassId())
                .classCode(group.getAcademicClass().getClassCode())
                .groupName(group.getGroupName())
                .courseCode(group.getAcademicClass().getCourse().getCourseCode())
                .semesterCode(group.getAcademicClass().getSemester().getSemesterCode())
                .createdAt(group.getCreatedAt())
                .build();

        lecturerAssignmentRepository.findById(group.getAcademicClass().getClassId()).ifPresent(la -> {
            resp.setLecturerId(la.getLecturerId());
            userRepository.findById(la.getLecturerId()).ifPresent(u -> resp.setLecturerName(u.getFullName()));
        });

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            userRepository.findByUsername(auth.getName()).ifPresent(user -> {
                GroupMemberId memberId = new GroupMemberId(group.getGroupId(), user.getUserId());
                groupMemberRepository.findById(memberId).ifPresent(member -> {
                    resp.setMemberRole(member.getMemberRole().getCode()); // "LEADER" or "MEMBER"
                });
            });
        }

        return resp;
    }
}