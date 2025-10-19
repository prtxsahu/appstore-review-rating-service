package com.example.appstore.shared.exception;

/**
 * Exception thrown when a requested resource is not found.
 * This exception will be handled by the GlobalExceptionHandler
 * and return a 404 Not Found response.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
