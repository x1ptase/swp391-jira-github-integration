package com.swp391.backend.service.impl;

import com.swp391.backend.entity.AcademicClass;
import com.swp391.backend.entity.LecturerAssignment;

import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.entity.User;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.AcademicClassRepository;
import com.swp391.backend.repository.LecturerAssignmentRepository;
import com.swp391.backend.repository.StudentGroupRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.service.LecturerAssignmentService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LecturerAssignmentServiceImpl implements LecturerAssignmentService {

    private final LecturerAssignmentRepository lecturerAssignmentRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final AcademicClassRepository academicClassRepository;
    private final UserRepository userRepository;

    public LecturerAssignmentServiceImpl(
            LecturerAssignmentRepository lecturerAssignmentRepository,
            StudentGroupRepository studentGroupRepository,
            AcademicClassRepository academicClassRepository,
            UserRepository userRepository
    ) {
        this.lecturerAssignmentRepository = lecturerAssignmentRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.academicClassRepository = academicClassRepository;
        this.userRepository = userRepository;
    }

    public void assignLecturer(Long classId, Long lecturerId) {
        // validate class exists
        Optional<AcademicClass> classOpt = academicClassRepository.findById(classId);
        if (!classOpt.isPresent()) {
            throw new BusinessException("Class not found: " + classId, 404);
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
            la.setClassId(classId);
        }
        la.setLecturerId(lecturerId);
        la.setAssignedAt(LocalDateTime.now());
        lecturerAssignmentRepository.save(la);
    }

    public List<StudentGroup> getAssignedGroupsForCurrentLecturer() {
        Long lecturerId = currentUserId();
        if (lecturerId == null) {
            throw new BusinessException("Unauthorized", 401);
        }

        List<LecturerAssignment> assigns = lecturerAssignmentRepository.findByLecturerId(lecturerId);
        List<StudentGroup> result = new ArrayList<StudentGroup>();

        for (int i = 0; i < assigns.size(); i++) {
            LecturerAssignment a = assigns.get(i);
            List<StudentGroup> classGroups = studentGroupRepository.findByAcademicClass_ClassId(a.getClassId());
            result.addAll(classGroups);
        }
        return result;
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
