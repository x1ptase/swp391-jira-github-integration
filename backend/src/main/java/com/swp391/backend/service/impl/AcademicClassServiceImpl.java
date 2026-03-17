package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.entity.*;
import com.swp391.backend.repository.*;
import com.swp391.backend.service.AcademicClassService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AcademicClassServiceImpl implements AcademicClassService {

    private final AcademicClassRepository academicClassRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final LecturerAssignmentRepository lecturerAssignmentRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;

    public AcademicClassServiceImpl(
            AcademicClassRepository academicClassRepository,
            CourseRepository courseRepository,
            SemesterRepository semesterRepository,
            LecturerAssignmentRepository lecturerAssignmentRepository,
            StudentGroupRepository studentGroupRepository,
            UserRepository userRepository
    ) {
        this.academicClassRepository = academicClassRepository;
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
        this.lecturerAssignmentRepository = lecturerAssignmentRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.userRepository = userRepository;
    }

    // SEARCH CLASS
    @Override
    public Page<AcademicClassResponse> searchClasses(String keyword, String courseCode, String semesterCode, Pageable pageable) {
        Page<AcademicClass> page = academicClassRepository.searchClasses(keyword, courseCode, semesterCode, pageable);
        return page.map(this::toResponse);
    }

    // CREATE CLASS
    @Override
    public AcademicClassResponse createClass(String classCode, Long courseId, Long semesterId) {

        if (academicClassRepository.findByClassCode(classCode).isPresent()) {
            throw new RuntimeException("Class code already exists");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new RuntimeException("Semester not found"));

        AcademicClass c = new AcademicClass();
        c.setClassCode(classCode);
        c.setCourse(course);
        c.setSemester(semester);

        academicClassRepository.save(c);

        return toResponse(c);
    }

    // GET CLASS DETAIL
    @Override
    public AcademicClassResponse getClass(Long id) {

        AcademicClass c = academicClassRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        return toResponse(c);
    }

    // UPDATE CLASS
    @Override
    public AcademicClassResponse updateClass(Long id, String classCode, Long courseId, Long semesterId) {

        AcademicClass c = academicClassRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new RuntimeException("Semester not found"));

        c.setClassCode(classCode);
        c.setCourse(course);
        c.setSemester(semester);

        academicClassRepository.save(c);

        return toResponse(c);
    }

    // DELETE CLASS
    @Override
    public void deleteClass(Long id) {

        AcademicClass c = academicClassRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        if (lecturerAssignmentRepository.existsByClassId(id)) {
            throw new RuntimeException("Cannot delete class because lecturer assigned");
        }

        if (studentGroupRepository.existsByAcademicClass_ClassId(id)) {
            throw new RuntimeException("Cannot delete class because student groups exist");
        }

        academicClassRepository.delete(c);
    }

    // CONVERT ENTITY -> RESPONSE
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