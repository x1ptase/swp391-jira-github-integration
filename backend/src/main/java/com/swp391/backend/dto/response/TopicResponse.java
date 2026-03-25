package com.swp391.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class TopicResponse {

    private Long topicId;
    private String topicCode;
    private String topicName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long semesterId;
    private String semesterCode;
    private String semesterName;

    public TopicResponse() {}

}