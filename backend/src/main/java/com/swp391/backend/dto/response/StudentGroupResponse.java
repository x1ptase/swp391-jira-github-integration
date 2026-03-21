package com.swp391.backend.dto.response;

import java.time.LocalDateTime;

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

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getSemesterCode() { return semesterCode; }
    public void setSemesterCode(String semesterCode) { this.semesterCode = semesterCode; }
    public Long getLecturerId() { return lecturerId; }
    public void setLecturerId(Long lecturerId) { this.lecturerId = lecturerId; }
    public String getLecturerName() { return lecturerName; }
    public void setLecturerName(String lecturerName) { this.lecturerName = lecturerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

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
