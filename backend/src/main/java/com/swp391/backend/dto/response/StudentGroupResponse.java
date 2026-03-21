package com.swp391.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class StudentGroupResponse {
    private Long groupId;
    private Long classId;
    private String classCode;
    private String groupName;
    private String courseCode;
    private String semesterCode;
    private Long lecturerId;
    private String lecturerName;
    private String status;
    private String memberRole;
    private Long topicId;
    private String topicName;
    private LocalDateTime createdAt;

    public StudentGroupResponse() {
    }

    public static StudentGroupResponseBuilder builder() {
        return new StudentGroupResponseBuilder();
    }

    public static class StudentGroupResponseBuilder {
        private StudentGroupResponse instance = new StudentGroupResponse();

        public StudentGroupResponseBuilder groupId(Long groupId) { instance.setGroupId(groupId); return this; }
        public StudentGroupResponseBuilder classId(Long classId) { instance.setClassId(classId); return this; }
        public StudentGroupResponseBuilder classCode(String classCode) { instance.setClassCode(classCode); return this; }
        public StudentGroupResponseBuilder groupName(String groupName) { instance.setGroupName(groupName); return this; }
        public StudentGroupResponseBuilder courseCode(String courseCode) { instance.setCourseCode(courseCode); return this; }
        public StudentGroupResponseBuilder semesterCode(String semesterCode) { instance.setSemesterCode(semesterCode); return this; }
        public StudentGroupResponseBuilder lecturerId(Long lecturerId) { instance.setLecturerId(lecturerId); return this; }
        public StudentGroupResponseBuilder lecturerName(String lecturerName) { instance.setLecturerName(lecturerName); return this; }
        public StudentGroupResponseBuilder status(String status) { instance.setStatus(status); return this; }
        public StudentGroupResponseBuilder memberRole(String memberRole) { instance.setMemberRole(memberRole); return this; }
        public StudentGroupResponseBuilder topicId(Long topicId) { instance.setTopicId(topicId); return this; }
        public StudentGroupResponseBuilder topicName(String topicName) { instance.setTopicName(topicName); return this; }
        public StudentGroupResponseBuilder createdAt(LocalDateTime createdAt) { instance.setCreatedAt(createdAt); return this; }
        public StudentGroupResponse build() { return instance; }
    }
}
