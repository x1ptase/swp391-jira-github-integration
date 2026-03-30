package com.swp391.backend.exception;

/**
 * Exception được throw khi input monitoring filter không hợp lệ.
 * <p>
 * Ví dụ: fromDate &gt; toDate, lastNDays âm, status không hợp lệ.
 * HTTP status: 400 Bad Request.
 */
public class MonitoringValidationException extends RuntimeException {

    private final int status;

    public MonitoringValidationException(String message) {
        super(message);
        this.status = 400;
    }

    public MonitoringValidationException(String message, Throwable cause) {
        super(message, cause);
        this.status = 400;
    }

    public int getStatus() {
        return status;
    }
}
