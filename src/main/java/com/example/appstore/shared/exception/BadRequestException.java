package com.example.appstore.shared.exception;

/**
 * Exception thrown when a request contains invalid or missing parameters.
 * Results in a 400 Bad Request response.
 */
public class BadRequestException extends RuntimeException {
    
    public BadRequestException(String message) {
        super(message);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
