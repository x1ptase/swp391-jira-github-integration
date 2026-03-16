package com.swp391.backend.service.impl;

import com.swp391.backend.dto.response.AcademicClassResponse;
import com.swp391.backend.entity.AcademicClass;
import com.swp391.backend.repository.AcademicClassRepository;
import com.swp391.backend.service.AcademicClassService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AcademicClassServiceImpl implements AcademicClassService {

    private final AcademicClassRepository academicClassRepository;

    public AcademicClassServiceImpl(AcademicClassRepository academicClassRepository) {
        this.academicClassRepository = academicClassRepository;
    }

    @Override
    public Page<AcademicClassResponse> searchClasses(String keyword, String courseCode, String semesterCode, Pageable pageable) {
        Page<AcademicClass> page = academicClassRepository.searchClasses(keyword, courseCode, semesterCode, pageable);

        return page.map(this::toResponse);
    }

    private AcademicClassResponse toResponse(AcademicClass c) {
        return new AcademicClassResponse(
                c.getClassId(),
                c.getClassCode(),
                c.getCourse() != null ? c.getCourse().getCourseId() : null,
                c.getCourse() != null ? c.getCourse().getCourseCode() : null,
                c.getCourse() != null ? c.getCourse().getCourseName() : null,
                c.getSemester() != null ? c.getSemester().getSemesterId() : null,
                c.getSemester() != null ? c.getSemester().getSemesterCode() : null,
                c.getSemester() != null ? c.getSemester().getSemesterName() : null
        );
    }
}