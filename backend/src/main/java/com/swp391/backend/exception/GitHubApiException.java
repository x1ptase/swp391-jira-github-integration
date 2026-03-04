package com.swp391.backend.exception;

public class GitHubApiException extends BusinessException {
    public GitHubApiException(String message, int status) {
        super(message, status);
    }
}
