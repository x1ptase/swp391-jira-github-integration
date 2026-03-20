package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.UserResponse;
import com.swp391.backend.entity.AcademicClass;
import com.swp391.backend.entity.StudentClassAssignment;
import com.swp391.backend.entity.StudentClassAssignmentId;
import com.swp391.backend.entity.User;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.AcademicClassRepository;
import com.swp391.backend.repository.StudentClassAssignmentRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.repository.LecturerAssignmentRepository;
import com.swp391.backend.repository.GroupMemberRepository;
import com.swp391.backend.security.SecurityService;
import com.swp391.backend.service.ClassStudentAssignmentService;
import com.swp391.backend.entity.GroupMember;
import com.swp391.backend.dto.response.StudentClassDetailsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClassStudentAssignmentServiceImpl implements ClassStudentAssignmentService {

    private final AcademicClassRepository academicClassRepository;
    private final StudentClassAssignmentRepository studentClassAssignmentRepository;
    private final UserRepository userRepository;
    private final LecturerAssignmentRepository lecturerAssignmentRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SecurityService securityService;

    public ClassStudentAssignmentServiceImpl(
            AcademicClassRepository academicClassRepository,
            StudentClassAssignmentRepository studentClassAssignmentRepository,
            UserRepository userRepository,
            LecturerAssignmentRepository lecturerAssignmentRepository,
            GroupMemberRepository groupMemberRepository,
            SecurityService securityService
    ) {
        this.academicClassRepository = academicClassRepository;
        this.studentClassAssignmentRepository = studentClassAssignmentRepository;
        this.userRepository = userRepository;
        this.lecturerAssignmentRepository = lecturerAssignmentRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.securityService = securityService;
    }

    @Override
    @Transactional
    public void assignStudent(Long classId, Long studentId) {
        AcademicClass academicClass = academicClassRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("Class not found.", 404));

        if (!"OPEN".equalsIgnoreCase(academicClass.getStatus())) {
            throw new BusinessException("Class is closed.", 409);
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("Student not found.", 404));

        if (student.getRole() == null || student.getRole().getRoleCode() == null
                || !"STUDENT".equalsIgnoreCase(student.getRole().getRoleCode())) {
            throw new BusinessException("Only student can be assigned to class.", 400);
        }

        if (studentClassAssignmentRepository
                .existsByAcademicClass_ClassIdAndStudent_UserId(classId, studentId)) {
            throw new BusinessException("Student already assigned to this class.", 409);
        }

        if (studentClassAssignmentRepository.existsByStudent_UserId(studentId)) {
            throw new BusinessException("Student already assigned to another class.", 409);
        }

        long currentCount = studentClassAssignmentRepository.countByAcademicClass_ClassId(classId);
        if (currentCount >= 10) {
            throw new BusinessException("Class is already full.", 409);
        }

        StudentClassAssignment assignment = new StudentClassAssignment();
        assignment.setId(new StudentClassAssignmentId(classId, studentId));
        assignment.setAcademicClass(academicClass);
        assignment.setStudent(student);

        studentClassAssignmentRepository.save(assignment);

        long newCount = studentClassAssignmentRepository.countByAcademicClass_ClassId(classId);
        if (newCount >= 10) {
            academicClass.setStatus("CLOSED");
            academicClassRepository.save(academicClass);
        }
    }

    @Override
    @Transactional
    public void unassignStudent(Long classId, Long studentId) {
        boolean exists = studentClassAssignmentRepository
                .existsByAcademicClass_ClassIdAndStudent_UserId(classId, studentId);

        if (!exists) {
            throw new BusinessException("Student is not assigned to this class.", 404);
        }

        studentClassAssignmentRepository
                .deleteByAcademicClass_ClassIdAndStudent_UserId(classId, studentId);

        AcademicClass academicClass = academicClassRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("Class not found.", 404));

        long count = studentClassAssignmentRepository.countByAcademicClass_ClassId(classId);
        if (count < 10 && !"OPEN".equalsIgnoreCase(academicClass.getStatus())) {
            academicClass.setStatus("OPEN");
            academicClassRepository.save(academicClass);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listStudentsInClass(Long classId) {
        return studentClassAssignmentRepository.findByAcademicClass_ClassId(classId)
                .stream()
                .map(a -> {
                    User s = a.getStudent();

                    UserResponse res = new UserResponse();
                    res.setUserId(s.getUserId());
                    res.setUsername(s.getUsername());
                    res.setFullName(s.getFullName());
                    res.setEmail(s.getEmail());
                    res.setStudentCode(s.getStudentCode());
                    res.setGithubUsername(s.getGithubUsername());
                    res.setJiraAccountId(s.getJiraAccountId());
                    res.setRoleCode(s.getRole() != null ? s.getRole().getRoleCode() : null);
                    res.setCreatedAt(s.getCreatedAt());

                    return res;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentClassDetailsResponse> searchStudents(Long classId, String search, Boolean hasGroup, Pageable pageable) {
        requireCanManageClass(classId);

        Page<User> usersPage = studentClassAssignmentRepository.searchStudentsInClass(classId, search, hasGroup, pageable);

        return usersPage.map(user -> {
            StudentClassDetailsResponse.StudentClassDetailsResponseBuilder builder = StudentClassDetailsResponse.builder()
                    .userId(user.getUserId())
                    .studentCode(user.getStudentCode())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .email(user.getEmail());

            Optional<GroupMember> groupMemberOpt = groupMemberRepository.findByUser_UserId(user.getUserId());
            GroupMember classGroupMember = groupMemberOpt
                    .filter(gm -> gm.getGroup().getAcademicClass().getClassId().equals(classId))
                    .orElse(null);

            if (classGroupMember != null) {
                builder.groupId(classGroupMember.getGroup().getGroupId())
                       .groupName(classGroupMember.getGroup().getGroupName())
                       .memberRole(classGroupMember.getMemberRole() != null ? classGroupMember.getMemberRole().getCode() : null);
            }

            return builder.build();
        });
    }

    private void requireCanManageClass(Long classId) {
        Long currentUserId = securityService.getCurrentUserId();
        if (currentUserId == null) throw new BusinessException("Unauthorized", 401);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        boolean assigned = lecturerAssignmentRepository.existsByClassIdAndLecturerId(classId, currentUserId);
        if (!assigned) {
            throw new BusinessException("Access denied. Lecturer not assigned to this class.", 403);
        }
    }
}