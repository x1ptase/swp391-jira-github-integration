package com.swp391.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationResponseDTO {
    private Long id;
    private String name;
    private String source;
    private boolean hasToken;
    private String tokenMasked;
}
