package com.swp391.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateTopicRequest {

    @NotBlank(message = "Topic code is required")
    @Size(max = 50, message = "Topic code max length is 50")
    private String topicCode;

    @NotBlank(message = "Topic name is required")
    @Size(max = 255, message = "Topic name max length is 255")
    private String topicName;

    private String description;

    public CreateTopicRequest() {}

    public String getTopicCode() { return topicCode; }
    public void setTopicCode(String topicCode) { this.topicCode = topicCode; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}