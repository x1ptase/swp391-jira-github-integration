package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.entity.*;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.AcademicClassRepository;
import com.swp391.backend.repository.ClassEnrollmentRepository;
import com.swp391.backend.repository.LecturerAssignmentRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.service.ClassEnrollmentService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassEnrollmentServiceImpl implements ClassEnrollmentService {

    private final AcademicClassRepository academicClassRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserRepository userRepository;
    private final LecturerAssignmentRepository lecturerAssignmentRepository;

    public ClassEnrollmentServiceImpl(
            AcademicClassRepository academicClassRepository,
            ClassEnrollmentRepository classEnrollmentRepository,
            UserRepository userRepository,
            LecturerAssignmentRepository lecturerAssignmentRepository
    ) {
        this.academicClassRepository = academicClassRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
        this.userRepository = userRepository;
        this.lecturerAssignmentRepository = lecturerAssignmentRepository;
    }

    @Override
    @Transactional
    public void enroll(Long classId) {
        User student = currentUser();

        if (student == null) {
            throw new BusinessException("Unauthorized", 401);
        }

        if (student.getRole() == null || student.getRole().getRoleCode() == null
                || !"STUDENT".equalsIgnoreCase(student.getRole().getRoleCode())) {
            throw new BusinessException("Only student can enroll class.", 403);
        }

        AcademicClass academicClass = academicClassRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("Class not found.", 404));

        boolean exists = classEnrollmentRepository.existsByAcademicClass_ClassIdAndStudent_UserId(
                classId, student.getUserId());

        if (exists) {
            throw new BusinessException("You already enrolled in this class.", 409);
        }

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setId(new ClassEnrollmentId(classId, student.getUserId()));
        enrollment.setAcademicClass(academicClass);
        enrollment.setStudent(student);

        classEnrollmentRepository.save(enrollment);
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

        ClassEnrollment enrollment = classEnrollmentRepository.findByStudent_UserId(student.getUserId())
                .orElseThrow(() -> new BusinessException("Student has not enrolled in any class.", 404));

        return toResponse(enrollment.getAcademicClass());
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        String username = auth.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private AcademicClassResponse toResponse(AcademicClass c) {
        var assignment = lecturerAssignmentRepository.findByClassId(c.getClassId());
        Long lecturerId = assignment.map(LecturerAssignment::getLecturerId).orElse(null);
        String lecturerName = lecturerId != null
                ? userRepository.findById(lecturerId).map(User::getFullName).orElse(null)
                : null;

        return new AcademicClassResponse(
                c.getClassId(),
                c.getClassCode(),
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