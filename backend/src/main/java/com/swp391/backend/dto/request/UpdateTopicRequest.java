package com.swp391.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateTopicRequest {

    @NotBlank(message = "Topic name is required")
    @Size(max = 255, message = "Topic name max length is 255")
    private String topicName;

    private String description;

    public UpdateTopicRequest() {}

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}