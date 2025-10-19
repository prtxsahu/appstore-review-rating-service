package com.example.appstore.shared.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized error response format for all API endpoints.
 * Provides consistent error structure across the application.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * Timestamp when the error occurred.
     */
    private Instant timestamp;
    
    /**
     * HTTP status code.
     */
    private int status;
    
    /**
     * Error type/category.
     */
    private String error;
    
    /**
     * Human-readable error message.
     */
    private String message;
    
    /**
     * Request path where the error occurred.
     */
    private String path;
    
    /**
     * Additional error details (e.g., validation errors).
     */
    private Map<String, Object> details;
}
