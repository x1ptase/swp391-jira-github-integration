package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.entity.AcademicClass;
import com.swp391.backend.entity.LecturerAssignment;

import com.swp391.backend.entity.User;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.AcademicClassRepository;
import com.swp391.backend.repository.LecturerAssignmentRepository;
import com.swp391.backend.repository.UserRepository;
import com.swp391.backend.service.AcademicClassService;
import com.swp391.backend.service.LecturerAssignmentService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LecturerAssignmentServiceImpl implements LecturerAssignmentService {

    private final LecturerAssignmentRepository lecturerAssignmentRepository;
    private final AcademicClassRepository academicClassRepository;
    private final UserRepository userRepository;
    private final AcademicClassService academicClassService;

    public LecturerAssignmentServiceImpl(
            LecturerAssignmentRepository lecturerAssignmentRepository,
            AcademicClassRepository academicClassRepository,
            UserRepository userRepository,
            AcademicClassService academicClassService
    ) {
        this.lecturerAssignmentRepository = lecturerAssignmentRepository;
        this.academicClassRepository = academicClassRepository;
        this.userRepository = userRepository;
        this.academicClassService = academicClassService;
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
            la.setClassId(classId);
        }
        la.setLecturerId(lecturerId);
        la.setAssignedAt(LocalDateTime.now());
        lecturerAssignmentRepository.save(la);
    }

    public List<AcademicClassResponse> getAssignedClassesForCurrentLecturer() {
        Long lecturerId = currentUserId();
        if (lecturerId == null) throw new BusinessException("Unauthorized", 401);

        return lecturerAssignmentRepository.findByLecturerId(lecturerId)
                .stream()
                .map(a -> academicClassService.getClass(a.getClassId()))
                .collect(Collectors.toList());
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
