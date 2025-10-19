package com.example.appstore.user.service;

import com.example.appstore.user.dto.CreateUserRequest;
import com.example.appstore.user.dto.UserResponse;

/**
 * Interface for User service operations.
 * Defines the contract for user management business logic.
 */
public interface UserServiceInterface {
    
    /**
     * Create a new user.
     * 
     * @param request The user creation request
     * @return The created user response
     */
    UserResponse createUser(CreateUserRequest request);
    
    /**
     * Get a user by their ID.
     * 
     * @param userId The user ID
     * @return The user response
     */
    UserResponse getUserById(String userId);
    
    /**
     * Check if a user exists.
     * 
     * @param userId The user ID to check
     * @return true if the user exists, false otherwise
     */
    boolean userExists(String userId);
}
