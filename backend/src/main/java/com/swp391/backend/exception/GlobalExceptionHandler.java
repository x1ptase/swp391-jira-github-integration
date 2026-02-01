package com.swp391.backend.exception;

import com.swp391.backend.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getStatus())
                .body(ApiResponse.error(e.getStatus(), e.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            org.springframework.security.authentication.BadCredentialsException e) {
        return ResponseEntity.status(401)
                .body(ApiResponse.error(401, "Invalid username or password"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "Internal Server Error: " + e.getMessage()));
    }
}
