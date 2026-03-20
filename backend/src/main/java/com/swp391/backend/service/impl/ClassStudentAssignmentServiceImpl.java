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
import com.swp391.backend.service.ClassStudentAssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClassStudentAssignmentServiceImpl implements ClassStudentAssignmentService {

    private final AcademicClassRepository academicClassRepository;
    private final StudentClassAssignmentRepository studentClassAssignmentRepository;
    private final UserRepository userRepository;

    public ClassStudentAssignmentServiceImpl(
            AcademicClassRepository academicClassRepository,
            StudentClassAssignmentRepository studentClassAssignmentRepository,
            UserRepository userRepository
    ) {
        this.academicClassRepository = academicClassRepository;
        this.studentClassAssignmentRepository = studentClassAssignmentRepository;
        this.userRepository = userRepository;
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
}