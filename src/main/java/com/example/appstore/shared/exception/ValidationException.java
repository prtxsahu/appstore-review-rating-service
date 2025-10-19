package com.example.appstore.shared.exception;

/**
 * Exception thrown when request validation fails.
 * Results in a 400 Bad Request response.
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
