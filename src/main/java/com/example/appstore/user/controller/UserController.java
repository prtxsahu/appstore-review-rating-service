package com.example.appstore.user.controller;

import com.example.appstore.user.dto.CreateUserRequest;
import com.example.appstore.user.dto.UserResponse;
import com.example.appstore.user.service.UserServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user-related operations.
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceInterface userService;

    /**
     * Create a new user.
     * POST /users
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("=== USER CONTROLLER: Creating user ===");
        log.info("Request received - name: {}", request.getName());
        log.debug("Full request: {}", request);
        
        try {
            UserResponse response = userService.createUser(request);
            log.info("User created successfully - userId: {}, name: {}", response.getUserId(), response.getName());
            log.debug("Full response: {}", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Failed to create user - name: {}, error: {}", request.getName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get user by ID.
     * GET /users/{userId}
     */
    // Not an ideal way to get user by ID, we would normally use somehting like JWT token, but for demo project purpose, we established that this is good.
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        log.info("=== USER CONTROLLER: Getting user by ID ===");
        log.info("Request received - userId: {}", userId);
        
        try {
            UserResponse response = userService.getUserById(userId);
            log.info("User retrieved successfully - userId: {}, name: {}", response.getUserId(), response.getName());
            log.debug("Full response: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get user - userId: {}, error: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
}
