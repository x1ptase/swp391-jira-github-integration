package com.swp391.backend.service;

import com.swp391.backend.entity.Semester;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SemesterService {

    Semester createSemester(String code, String name, LocalDate startDate, LocalDate endDate);

    Page<Semester> listSemesters(Pageable pageable);

    Semester getSemester(Long id);

    Semester updateSemester(Long id, String name, LocalDate startDate, LocalDate endDate);

    void deleteSemester(Long id);

}