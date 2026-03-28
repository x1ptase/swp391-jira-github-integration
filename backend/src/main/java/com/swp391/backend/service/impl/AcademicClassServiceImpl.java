package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.dto.response.ClassSummaryResponse;
import com.swp391.backend.entity.*;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.*;
import com.swp391.backend.service.AcademicClassService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AcademicClassServiceImpl implements AcademicClassService {

    private final AcademicClassRepository academicClassRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final LecturerAssignmentRepository lecturerAssignmentRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final UserRepository userRepository;
    private final StudentClassAssignmentRepository studentClassAssignmentRepository;
    private final GroupMemberRepository groupMemberRepository;
    public AcademicClassServiceImpl(
            AcademicClassRepository academicClassRepository,
            CourseRepository courseRepository,
            SemesterRepository semesterRepository,
            LecturerAssignmentRepository lecturerAssignmentRepository,
            StudentGroupRepository studentGroupRepository,
            UserRepository userRepository,
            StudentClassAssignmentRepository studentClassAssignmentRepository,
            GroupMemberRepository groupMemberRepository
    ) {
        this.academicClassRepository = academicClassRepository;
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
        this.lecturerAssignmentRepository = lecturerAssignmentRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.userRepository = userRepository;
        this.studentClassAssignmentRepository = studentClassAssignmentRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    //SUMMARY CLASS
    @Override
    public ClassSummaryResponse getClassSummary(Long classId) {
        AcademicClass academicClass = academicClassRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("Class not found: " + classId, 404));

        long totalStudents = studentClassAssignmentRepository.countByAcademicClass_ClassId(classId);
        long totalGroups = studentGroupRepository.countByAcademicClass_ClassId(classId);
        long groupsWithTopic = studentGroupRepository.countByAcademicClass_ClassIdAndTopicIsNotNull(classId);
        long studentsWithoutGroup = groupMemberRepository.countStudentsWithoutGroupByClassId(classId);

        return ClassSummaryResponse.builder()
                .classId(academicClass.getClassId())
                .classCode(academicClass.getClassCode())
                .totalStudents(totalStudents)
                .totalGroups(totalGroups)
                .groupsWithAssignedTopic(groupsWithTopic)
                .topicAssignedSummary(groupsWithTopic + "/" + totalGroups)
                .studentsWithoutGroup(studentsWithoutGroup)
                .build();
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
            throw new BusinessException("Class code already exists", 409);
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException("Course not found", 404));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new BusinessException("Semester not found", 404));

        if (semester.getEndDate() != null && semester.getEndDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Cannot create class because the semester has ended", 409);
        }

        AcademicClass c = new AcademicClass();
        c.setClassCode(classCode);
        c.setStatus("OPEN");
        c.setCourse(course);
        c.setSemester(semester);

        academicClassRepository.save(c);

        return toResponse(c);
    }

    // GET CLASS DETAIL
    @Override
    public AcademicClassResponse getClass(Long id) {

        AcademicClass c = academicClassRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Class not found", 404));

        return toResponse(c);
    }

    // UPDATE CLASS
    @Override
    public AcademicClassResponse updateClass(Long id, String classCode, Long courseId, Long semesterId) {

        AcademicClass c = academicClassRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Class not found", 404));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException("Course not found", 404));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new BusinessException("Semester not found", 404));

        if (semester.getEndDate() != null && semester.getEndDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Cannot assign/update class to a semester that has already ended", 409);
        }

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
                .orElseThrow(() -> new BusinessException("Class not found", 404));

        if (lecturerAssignmentRepository.existsByClassId(id)) {
            throw new BusinessException("Cannot delete class because lecturer assigned", 409);
        }

        if (studentGroupRepository.existsByAcademicClass_ClassId(id)) {
            throw new BusinessException("Cannot delete class because student groups exist", 409);
        }

        academicClassRepository.delete(c);
    }

    // CONVERT ENTITY -> RESPONSE
    private AcademicClassResponse toResponse(AcademicClass c) {
        var assignment = lecturerAssignmentRepository.findByClassId(c.getClassId());
        Long lecturerId = assignment.map(la -> la.getLecturer().getUserId()).orElse(null);
        String lecturerName = assignment.map(la -> la.getLecturer().getFullName()).orElse(null);
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