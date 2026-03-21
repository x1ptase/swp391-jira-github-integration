package com.swp391.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateTopicRequest {

    @NotBlank(message = "Topic code is required")
    @Size(max = 50, message = "Topic code max length is 50")
    private String topicCode;

    @NotBlank(message = "Topic name is required")
    @Size(max = 255, message = "Topic name max length is 255")
    private String topicName;

    private String description;

    public CreateTopicRequest() {}

}