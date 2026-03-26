package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.entity.AcademicClass;
import com.swp391.backend.entity.LecturerAssignment;
import com.swp391.backend.entity.StudentClassAssignment;
import com.swp391.backend.entity.User;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.LecturerAssignmentRepository;
import com.swp391.backend.repository.StudentClassAssignmentRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.service.StudentClassQueryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentClassQueryServiceImpl implements StudentClassQueryService {

    private final StudentClassAssignmentRepository studentClassAssignmentRepository;
    private final UserRepository userRepository;
    private final LecturerAssignmentRepository lecturerAssignmentRepository;

    public StudentClassQueryServiceImpl(
            StudentClassAssignmentRepository studentClassAssignmentRepository,
            UserRepository userRepository,
            LecturerAssignmentRepository lecturerAssignmentRepository
    ) {
        this.studentClassAssignmentRepository = studentClassAssignmentRepository;
        this.userRepository = userRepository;
        this.lecturerAssignmentRepository = lecturerAssignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicClassResponse getMyClass() {
        User student = currentUser();

        if (student == null) {
            throw new BusinessException("Unauthorized", 401);
        }

        if (student.getRole() == null || student.getRole().getRoleCode() == null
                || !"STUDENT".equalsIgnoreCase(student.getRole().getRoleCode())) {
            throw new BusinessException("Only student can view own class.", 403);
        }

        StudentClassAssignment assignment = studentClassAssignmentRepository
                .findByStudent_UserId(student.getUserId())
                .orElseThrow(() -> new BusinessException("Student is not assigned to any class.", 404));

        return toResponse(assignment.getAcademicClass());
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String username = auth.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private AcademicClassResponse toResponse(AcademicClass c) {
        var lecturerAssignment = lecturerAssignmentRepository.findByClassId(c.getClassId());
        Long lecturerId = lecturerAssignment.map(la -> la.getLecturer().getUserId()).orElse(null);
        String lecturerName = lecturerAssignment.map(la -> la.getLecturer().getFullName()).orElse(null);

        return new AcademicClassResponse(
                c.getClassId(),
                c.getClassCode(),
                c.getStatus(),
                c.getCourse() != null ? c.getCourse().getCourseId() : null,
                c.getCourse() != null ? c.getCourse().getCourseCode() : null,
                c.getCourse() != null ? c.getCourse().getCourseName() : null,
                c.getSemester() != null ? c.getSemester().getSemesterId() : null,
                c.getSemester() != null ? c.getSemester().getSemesterCode() : null,
                c.getSemester() != null ? c.getSemester().getSemesterName() : null,
                lecturerId,
                lecturerName
        );
    }
}