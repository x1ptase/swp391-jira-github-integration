package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.dto.response.GroupMemberResponse;
import com.swp391.backend.dto.response.LecturerGroupSummaryResponse;
import com.swp391.backend.entity.AcademicClass;
import com.swp391.backend.entity.GroupMember;
import com.swp391.backend.entity.LecturerAssignment;
import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.entity.User;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.AcademicClassRepository;
import com.swp391.backend.repository.GroupMemberRepository;
import com.swp391.backend.repository.LecturerAssignmentRepository;
import com.swp391.backend.repository.StudentGroupRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.service.AcademicClassService;
import com.swp391.backend.service.LecturerAssignmentService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LecturerAssignmentServiceImpl implements LecturerAssignmentService {

    private final LecturerAssignmentRepository lecturerAssignmentRepository;
    private final AcademicClassRepository academicClassRepository;
    private final UserRepository userRepository;
    private final AcademicClassService academicClassService;
    private final StudentGroupRepository studentGroupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public LecturerAssignmentServiceImpl(
            LecturerAssignmentRepository lecturerAssignmentRepository,
            AcademicClassRepository academicClassRepository,
            UserRepository userRepository,
            AcademicClassService academicClassService,
            StudentGroupRepository studentGroupRepository,
            GroupMemberRepository groupMemberRepository
    ) {
        this.lecturerAssignmentRepository = lecturerAssignmentRepository;
        this.academicClassRepository = academicClassRepository;
        this.userRepository = userRepository;
        this.academicClassService = academicClassService;
        this.studentGroupRepository = studentGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    public void assignLecturer(Long classId, Long lecturerId) {
        // validate class exists
        Optional<AcademicClass> classOpt = academicClassRepository.findById(classId);
        if (!classOpt.isPresent()) {
            throw new BusinessException("Class not found: " + classId, 404);
        }

        // nếu lecturerId = null => unassign
        if (lecturerId == null) {
            Optional<LecturerAssignment> existing = lecturerAssignmentRepository.findById(classId);
            if (existing.isPresent()) {
                lecturerAssignmentRepository.delete(existing.get());
            }
            return;
        }

        // validate lecturer exists + role is LECTURER
        Optional<User> lectOpt = userRepository.findById(lecturerId);
        if (!lectOpt.isPresent()) {
            throw new BusinessException("Lecturer not found: " + lecturerId, 404);
        }
        User lect = lectOpt.get();
        if (lect.getRole() == null || lect.getRole().getRoleCode() == null
                || !"LECTURER".equalsIgnoreCase(lect.getRole().getRoleCode())) {
            throw new BusinessException("User is not a lecturer: " + lecturerId, 400);
        }

        // upsert
        Optional<LecturerAssignment> existing = lecturerAssignmentRepository.findById(classId);
        LecturerAssignment la;
        if (existing.isPresent()) {
            la = existing.get();
        } else {
            la = new LecturerAssignment();
            la.setAcademicClass(classOpt.get());
        }
        la.setLecturer(lectOpt.get());
        la.setAssignedAt(LocalDateTime.now());
        lecturerAssignmentRepository.save(la);
    }

    public List<AcademicClassResponse> getAssignedClassesForCurrentLecturer() {
        Long lecturerId = currentUserId();
        if (lecturerId == null) throw new BusinessException("Unauthorized", 401);

        return lecturerAssignmentRepository.findByLecturer_UserId(lecturerId)
                .stream()
                .map(a -> academicClassService.getClass(a.getClassId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<LecturerGroupSummaryResponse> getGroupsForCurrentLecturer() {
        Long lecturerId = currentUserId();
        if (lecturerId == null) throw new BusinessException("Unauthorized", 401);

        List<StudentGroup> groups = studentGroupRepository.findByLecturerId(lecturerId);

        List<LecturerGroupSummaryResponse> result = new ArrayList<>();
        for (StudentGroup sg : groups) {
            List<GroupMember> rawMembers = groupMemberRepository.findByGroup_GroupId(sg.getGroupId());

            List<GroupMemberResponse> memberDtos = new ArrayList<>();
            for (GroupMember gm : rawMembers) {
                GroupMemberResponse dto = new GroupMemberResponse();
                dto.setUserId(gm.getUser().getUserId());
                dto.setUsername(gm.getUser().getUsername());
                dto.setFullName(gm.getUser().getFullName());
                dto.setEmail(gm.getUser().getEmail());
                dto.setMemberRole(gm.getMemberRole() == null ? null : gm.getMemberRole().getCode());
                dto.setStudentCode(gm.getUser().getStudentCode());
                memberDtos.add(dto);
            }

            LecturerGroupSummaryResponse summary = LecturerGroupSummaryResponse.builder()
                    .groupId(sg.getGroupId())
                    .groupName(sg.getGroupName())
                    .classCode(sg.getAcademicClass() != null ? sg.getAcademicClass().getClassCode() : null)
                    .semesterCode(sg.getAcademicClass() != null && sg.getAcademicClass().getSemester() != null
                            ? sg.getAcademicClass().getSemester().getSemesterCode() : null)
                    .topicName(sg.getTopic() != null ? sg.getTopic().getTopicName() : null)
                    .members(memberDtos)
                    .build();

            result.add(summary);
        }
        return result;
    }

    @Override
    public List<LecturerGroupSummaryResponse> getGroupsForCurrentLecturerByClass(Long classId) {
        Long lecturerId = currentUserId();
        if (lecturerId == null) {
            throw new BusinessException("Unauthorized", 401);
        }

        if (!academicClassRepository.existsById(classId)) {
            throw new BusinessException("Class not found: " + classId, 404);
        }

        boolean isAssigned = lecturerAssignmentRepository.existsByClassIdAndLecturer_UserId(classId, lecturerId);
        if (!isAssigned) {
            throw new BusinessException("You are not assigned to this class", 403);
        }

        List<StudentGroup> groups = studentGroupRepository.findByAcademicClass_ClassId(classId);

        return groups.stream()
                .map(this::mapToLecturerGroupSummary)
                .collect(Collectors.toList());
    }

    private LecturerGroupSummaryResponse mapToLecturerGroupSummary(StudentGroup sg) {
        List<GroupMember> rawMembers = groupMemberRepository.findByGroup_GroupId(sg.getGroupId());

        List<GroupMemberResponse> memberDtos = new ArrayList<>();
        for (GroupMember gm : rawMembers) {
            GroupMemberResponse dto = new GroupMemberResponse();
            dto.setUserId(gm.getUser().getUserId());
            dto.setUsername(gm.getUser().getUsername());
            dto.setFullName(gm.getUser().getFullName());
            dto.setEmail(gm.getUser().getEmail());
            dto.setMemberRole(gm.getMemberRole() == null ? null : gm.getMemberRole().getCode());
            dto.setStudentCode(gm.getUser().getStudentCode());
            memberDtos.add(dto);
        }

        return LecturerGroupSummaryResponse.builder()
                .groupId(sg.getGroupId())
                .groupName(sg.getGroupName())
                .classCode(sg.getAcademicClass() != null ? sg.getAcademicClass().getClassCode() : null)
                .semesterCode(sg.getAcademicClass() != null && sg.getAcademicClass().getSemester() != null
                        ? sg.getAcademicClass().getSemester().getSemesterCode() : null)
                .topicName(sg.getTopic() != null ? sg.getTopic().getTopicName() : null)
                .members(memberDtos)
                .build();
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String username = auth.getName();
        Optional<User> uOpt = userRepository.findByUsername(username);
        if (!uOpt.isPresent()) return null;
        return uOpt.get().getUserId();
    }
}
