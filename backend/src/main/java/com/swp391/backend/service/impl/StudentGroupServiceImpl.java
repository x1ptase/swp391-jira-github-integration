package com.swp391.backend.service.impl;

import com.swp391.backend.dto.request.CreateGroupRequest;
import com.swp391.backend.dto.response.StudentGroupResponse;
import com.swp391.backend.entity.AcademicClass;
import com.swp391.backend.entity.GroupMember;
import com.swp391.backend.entity.GroupMemberId;
import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.AcademicClassRepository;
import com.swp391.backend.repository.GroupMemberRepository;
import com.swp391.backend.repository.LecturerAssignmentRepository;
import com.swp391.backend.repository.StudentGroupRepository;
import com.swp391.backend.repository.TopicRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.security.SecurityService;
import com.swp391.backend.service.StudentGroupService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentGroupServiceImpl implements StudentGroupService {
    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;
    private final LecturerAssignmentRepository lecturerAssignmentRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final AcademicClassRepository academicClassRepository;
    private final TopicRepository topicRepository;
    private final SecurityService securityService;

    public StudentGroupServiceImpl(StudentGroupRepository studentGroupRepository,
                                   UserRepository userRepository,
                                   LecturerAssignmentRepository lecturerAssignmentRepository,
                                   GroupMemberRepository groupMemberRepository,
                                   AcademicClassRepository academicClassRepository,
                                   TopicRepository topicRepository,
                                   SecurityService securityService) {
        this.studentGroupRepository = studentGroupRepository;
        this.userRepository = userRepository;
        this.lecturerAssignmentRepository = lecturerAssignmentRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.academicClassRepository = academicClassRepository;
        this.topicRepository = topicRepository;
        this.securityService = securityService;
    }

    @Override
    @Transactional
    public StudentGroupResponse addStudentGroup(CreateGroupRequest request) {

        AcademicClass clazz = academicClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new BusinessException("AcademicClass not found: " + request.getClassId(), 404));

        requireCanManageClass(clazz.getClassId());

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

        requireCanManageClass(existing.getAcademicClass().getClassId());

        if ("CLOSED".equals(existing.getStatus())) {
            throw new BusinessException("Cannot update group when status is CLOSED", 400);
        }

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

        requireCanManageClass(existing.getAcademicClass().getClassId());

        studentGroupRepository.delete(existing);
        return mapToResponse(existing);
    }

    @Override
    public List<StudentGroupResponse> listStudentGroups(String courseCode, String semesterCode, Long classId) {
        if (classId != null) {
            return studentGroupRepository.findByAcademicClass_ClassId(classId)
                    .stream().map(this::mapToResponse).collect(Collectors.toList());
        }
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
        resp.setStatus(group.getStatus());
        if (group.getTopic() != null) {
            resp.setTopicId(group.getTopic().getTopicId());
            resp.setTopicName(group.getTopic().getTopicName());
        }

        lecturerAssignmentRepository.findById(group.getAcademicClass().getClassId()).ifPresent(la -> {
            if (la.getLecturer() != null) {
                resp.setLecturerId(la.getLecturer().getUserId());
                resp.setLecturerName(la.getLecturer().getFullName());
            }
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

    @Override
    @Transactional
    public StudentGroupResponse changeGroupStatus(Long groupId, String status) {
        StudentGroup existing = studentGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("StudentGroup not found: " + groupId, 404));

        requireCanManageClass(existing.getAcademicClass().getClassId());

        existing.setStatus(status);
        return mapToResponse(studentGroupRepository.save(existing));
    }

    @Override
    @Transactional
    public StudentGroupResponse assignTopic(Long groupId, Long topicId) {
        StudentGroup existing = studentGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("StudentGroup not found: " + groupId, 404));

        requireCanManageClass(existing.getAcademicClass().getClassId());

        if (topicId == null) {
            existing.setTopic(null);
        } else {
            com.swp391.backend.entity.Topic topic = topicRepository.findById(topicId)
                    .orElseThrow(() -> new BusinessException("Topic not found: " + topicId, 404));

            Long groupSemesterId = existing.getAcademicClass().getSemester().getSemesterId();
            Long topicSemesterId = topic.getSemester().getSemesterId();

            if (!groupSemesterId.equals(topicSemesterId)) {
                throw new BusinessException("Topic does not belong to the same semester as the group", 409);
            }

            existing.setTopic(topic);
        }

        return mapToResponse(studentGroupRepository.save(existing));
    }

    private void requireCanManageClass(Long classId) {
        Long currentUserId = securityService.getCurrentUserId();
        if (currentUserId == null) throw new BusinessException("Unauthorized", 401);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        boolean assigned = lecturerAssignmentRepository.existsByClassIdAndLecturer_UserId(classId, currentUserId);
        if (!assigned) {
            throw new BusinessException("Access denied. Lecturer not assigned to this class.", 403);
        }
    }
}