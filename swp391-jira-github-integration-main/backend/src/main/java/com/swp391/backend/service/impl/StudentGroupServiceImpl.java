package com.swp391.backend.service.impl;

import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.exception.BusinessException;
import com.swp391.backend.repository.StudentGroupRepository;
import com.swp391.backend.service.StudentGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class StudentGroupServiceImpl implements StudentGroupService {
    private final StudentGroupRepository studentGroupRepository;

    public StudentGroupServiceImpl(StudentGroupRepository studentGroupRepository) {
        this.studentGroupRepository = studentGroupRepository;
    }

    @Override
    public StudentGroup addStudentGroup(StudentGroup studentGroup) {
        if (studentGroupRepository.existsByGroupCode(studentGroup.getGroupCode())) {
            throw new BusinessException("StudentGroup groupCode already exists: " + studentGroup.getGroupCode(), 409);
        }
        return studentGroupRepository.save(studentGroup);
    }

    @Override
    public StudentGroup updateStudentGroup(StudentGroup studentGroup , Long id) {
        StudentGroup existing = studentGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException("StudentGroup not found: " + id, 404));

        if (studentGroupRepository.existsByGroupCodeAndGroupIdNot(studentGroup.getGroupCode(), id)) {
            throw new BusinessException("StudentGroup groupCode already exists: " + studentGroup.getGroupCode(), 409);
        }

        existing.setGroupCode(studentGroup.getGroupCode());
        existing.setGroupName(studentGroup.getGroupName());
        existing.setCourseCode(studentGroup.getCourseCode());
        existing.setSemester(studentGroup.getSemester());

        return studentGroupRepository.save(existing);
    }

    @Override
    @Transactional
    public StudentGroup deleteStudentGroup(Long studentGroupId) {
        studentGroupRepository.removeStudentGroupByGroupId(studentGroupId);
        return null;
    }

    @Override
    public List<StudentGroup> listStudentGroups(String courseCode, String semester) {
        String normalizedCourseCode = StringUtils.hasText(courseCode) ? courseCode.trim() : null;
        String normalizedSemester = StringUtils.hasText(semester) ? semester.trim() : null;

        if (normalizedCourseCode != null && normalizedSemester != null) {
            return studentGroupRepository.findByCourseCodeAndSemester(normalizedCourseCode, normalizedSemester);
        }

        if (normalizedCourseCode != null) {
            return studentGroupRepository.findByCourseCode(normalizedCourseCode);
        }
        if (normalizedSemester != null) {
            return studentGroupRepository.findBySemester(normalizedSemester);
        }

        return studentGroupRepository.findAll();
    }
}