package com.example.appstore.user.service;

import com.example.appstore.user.domain.User;
import com.example.appstore.user.dto.CreateUserRequest;
import com.example.appstore.user.dto.UserResponse;
import com.example.appstore.user.repository.dynamodb.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for User operations.
 * Handles business logic for user management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Create a new user.
     */
    public UserResponse createUser(CreateUserRequest request) {
        log.info("=== USER SERVICE: Creating user ===");
        log.info("Input - name: {}", request.getName());
        log.debug("Full request: {}", request);
        
        try {
            // Generate unique user ID
            String userId = "USER_" + UUID.randomUUID().toString();
            log.debug("Generated userId: {}", userId);
            
            // Create user entity
            User user = User.builder()
                    .userId(userId)
                    .name(request.getName())
                    .createdAt(Instant.now())
                    .build();
            log.debug("Created user entity: {}", user);
            
            // Save to DynamoDB
            log.info("Saving user to DynamoDB - userId: {}", userId);
            User savedUser = userRepository.save(user);
            log.info("User saved to DynamoDB successfully - userId: {}", savedUser.getUserId());
            log.debug("Saved user entity: {}", savedUser);
            
            // Convert to response DTO
            UserResponse response = UserResponse.builder()
                    .userId(savedUser.getUserId())
                    .name(savedUser.getName())
                    .createdAt(savedUser.getCreatedAt())
                    .build();
            
            log.info("User creation completed successfully - userId: {}, name: {}", response.getUserId(), response.getName());
            log.debug("Response DTO: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to create user - name: {}, error: {}", request.getName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get user by ID.
     */
    public UserResponse getUserById(String userId) {
        log.info("=== USER SERVICE: Getting user by ID ===");
        log.info("Input - userId: {}", userId);
        
        try {
            log.info("Fetching user from DynamoDB - userId: {}", userId);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User not found in DynamoDB - userId: {}", userId);
                        return new RuntimeException("User not found: " + userId);
                    });
            log.info("User found in DynamoDB - userId: {}, name: {}", user.getUserId(), user.getName());
            log.debug("Retrieved user entity: {}", user);
            
            // Convert to response DTO
            UserResponse response = UserResponse.builder()
                    .userId(user.getUserId())
                    .name(user.getName())
                    .createdAt(user.getCreatedAt())
                    .build();
            
            log.info("User retrieval completed successfully - userId: {}, name: {}", response.getUserId(), response.getName());
            log.debug("Response DTO: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to get user - userId: {}, error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check if user exists.
     */
    public boolean userExists(String userId) {
        log.info("=== USER SERVICE: Checking if user exists ===");
        log.info("Input - userId: {}", userId);
        
        try {
            boolean exists = userRepository.existsById(userId);
            log.info("User existence check completed - userId: {}, exists: {}", userId, exists);
            return exists;
        } catch (Exception e) {
            log.error("Failed to check user existence - userId: {}, error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
}
