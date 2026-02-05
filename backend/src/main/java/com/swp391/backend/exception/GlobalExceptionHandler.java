package com.swp391.backend.exception;

import com.swp391.backend.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Validation failed: ");

        List<FieldError> errors = e.getBindingResult().getFieldErrors();
        for (int i = 0; i < errors.size(); i++) {
            FieldError fe = errors.get(i);
            if (i > 0) sb.append("; ");
            sb.append(fe.getField()).append(" ").append(fe.getDefaultMessage());
        }

        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, sb.toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "Internal Server Error: " + e.getMessage()));
    }
}
