package com.swp391.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommitSearchRequest {
    private String fromDate;
    private String toDate;
    private Integer lastNDays;
}
