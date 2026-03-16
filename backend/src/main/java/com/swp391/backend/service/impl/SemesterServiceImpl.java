package com.swp391.backend.service.impl;

import com.swp391.backend.entity.Semester;
import com.swp391.backend.repository.AcademicClassRepository;
import com.swp391.backend.repository.SemesterRepository;
import com.swp391.backend.service.SemesterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SemesterServiceImpl implements SemesterService {

    private final SemesterRepository semesterRepository;
    private final AcademicClassRepository academicClassRepository;

    public SemesterServiceImpl(
            SemesterRepository semesterRepository,
            AcademicClassRepository academicClassRepository
    ) {
        this.semesterRepository = semesterRepository;
        this.academicClassRepository = academicClassRepository;
    }

    // LIST SEMESTER
    @Override
    public Page<Semester> listSemesters(Pageable pageable) {
        return semesterRepository.findAll(pageable);
    }

    // CREATE SEMESTER
    @Override
    public Semester createSemester(String code, String name, LocalDate startDate, LocalDate endDate) {

        if (semesterRepository.existsBySemesterCode(code)) {
            throw new RuntimeException("Semester code already exists");
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date must be before end date");
        }

        Semester semester = new Semester();
        semester.setSemesterCode(code);
        semester.setSemesterName(name);
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);

        return semesterRepository.save(semester);
    }

    // GET DETAIL
    @Override
    public Semester getSemester(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Semester not found"));
    }

    // UPDATE
    @Override
    public Semester updateSemester(Long id, String name, LocalDate startDate, LocalDate endDate) {

        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Semester not found"));

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new RuntimeException("Start date must be before end date");
        }

        semester.setSemesterName(name);
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);

        return semesterRepository.save(semester);
    }
}