package com.swp391.backend.exception;

import com.swp391.backend.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
            if (i > 0)
                sb.append("; ");
            sb.append(fe.getField()).append(" ").append(fe.getDefaultMessage());
        }

        return ResponseEntity.status(400)
                .body(ApiResponse.error(400, sb.toString()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity.status(404)
                .body(ApiResponse.error(404, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        return ResponseEntity.status(500)
                .body(ApiResponse.error(500, "Internal Server Error: " + e.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException e) {

        String msg = null;
        Throwable most = e.getMostSpecificCause();
        if (most != null) {
            msg = most.getMessage();
        }
        if (msg == null) {
            msg = e.getMessage();
        }
        if (msg == null) {
            msg = "";
        }

        // BR-01: Each student may join at most one group in the same course and semester
        if (containsIgnoreCase(msg, "UQ_OneGroupPerStudentPerTerm")) {
            return ResponseEntity.status(409)
                    .body(ApiResponse.error(409,
                            "BR-01 violated: Each student may join at most one group in the same course and semester."));
        }

        // BR-02: each group has at most 1 leader
        if (containsIgnoreCase(msg, "UX_Group_OneLeader")) {
            return ResponseEntity.status(409)
                    .body(ApiResponse.error(409,
                            "BR-02 violated: Each group has at most 1 leader."));
        }

        // Duplicate member in same group (PRIMARY KEY (group_id, user_id))
        if (containsIgnoreCase(msg, "PRIMARY KEY")
                && (containsIgnoreCase(msg, "GroupMember") || containsIgnoreCase(msg, "dbo.GroupMember"))) {
            return ResponseEntity.status(409)
                    .body(ApiResponse.error(409, "Member đã tồn tại trong group (duplicate group_id + user_id)."));
        }

        // fallback chung
        return ResponseEntity.status(409)
                .body(ApiResponse.error(409, "Conflict: Data integrity violation."));
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        if (text == null || keyword == null) return false;
        return text.toLowerCase().contains(keyword.toLowerCase());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ApiResponse.error(403, "Access Denied"));
    }

}
