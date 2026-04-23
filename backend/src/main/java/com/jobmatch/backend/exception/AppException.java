package com.jobmatch.backend.exception;

import org.springframework.http.HttpStatus;

import java.util.Objects;

public class AppException extends RuntimeException {

    private final HttpStatus status;

    // Constructor with message + status
    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    // Constructor with only message (optional but useful)
    public AppException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
