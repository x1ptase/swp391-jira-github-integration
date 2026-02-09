package com.swp391.backend.controller;

import com.swp391.backend.entity.StudentGroup;
import com.swp391.backend.service.impl.StudentGroupServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RequestMapping("/api/student_group")
@RestController
public class StudentGroupController {

    private final  StudentGroupServiceImpl studentGroupService;

    public StudentGroupController(StudentGroupServiceImpl studentGroupService) {
        this.studentGroupService = studentGroupService;
    }

    @PostMapping("/add")
    public ResponseEntity<StudentGroup> createStudentGroup(@Valid @RequestBody StudentGroup studentGroup) {
        StudentGroup stu = studentGroupService.addStudentGroup(studentGroup);
        return ResponseEntity.ok().body(stu);
    }
    @PutMapping("/update/{id}")
    public ResponseEntity<StudentGroup> updateStudentGroup(@Valid @RequestBody StudentGroup studentGroup , @PathVariable Long id) {
        StudentGroup stu = studentGroupService.updateStudentGroup(studentGroup , id);
        return ResponseEntity.ok().body(stu);
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<StudentGroup> deleteStudentGroup(@PathVariable Long id) {
        StudentGroup stu = studentGroupService.deleteStudentGroup(id);
        return ResponseEntity.ok().body(stu);
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<StudentGroup>> listStudentGroups(
            @RequestParam(value = "course_code", required = false) String courseCodeSnake,
            @RequestParam(value = "courseCode", required = false) String courseCodeCamel,
            @RequestParam(value = "semester", required = false) String semester
    ) {
        String courseCode = courseCodeSnake != null ? courseCodeSnake : courseCodeCamel;
        return ResponseEntity.ok(studentGroupService.listStudentGroups(courseCode, semester));
    }

    @GetMapping({"/filter", "/filter/"})
    public ResponseEntity<List<StudentGroup>> filterStudentGroups(
            @RequestParam(value = "course_code", required = false) String courseCodeSnake,
            @RequestParam(value = "courseCode", required = false) String courseCodeCamel,
            @RequestParam(value = "semester", required = false) String semester
    ) {
        String courseCode = courseCodeSnake != null ? courseCodeSnake : courseCodeCamel;
        return ResponseEntity.ok(studentGroupService.listStudentGroups(courseCode, semester));
    }

}
