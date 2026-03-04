package com.swp391.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JiraIssueExportDto {

    private String key;
    private String summary;
    private String description; // plain text (converted from ADF)
    private String issueType;
    private String status;
    private String priority; // nullable
    private String assignee; // nullable
    private String updated;
}
