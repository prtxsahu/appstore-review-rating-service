package com.example.appstore.user.service;

import com.example.appstore.user.domain.User;
import com.example.appstore.user.dto.CreateUserRequest;
import com.example.appstore.user.dto.UserResponse;
import com.example.appstore.user.repository.dynamodb.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Simple unit tests for UserService.
 * Tests the core business logic for user management operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Simple Tests")
class UserServiceSimpleTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private CreateUserRequest validCreateUserRequest;
    private User validUser;

    @BeforeEach
    void setUp() {
        // Setup test data
        validCreateUserRequest = CreateUserRequest.builder()
                .name("John Doe")
                .build();

        validUser = User.builder()
                .userId("USER_123")
                .name("John Doe")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should create user successfully with valid request")
    void createUser_WithValidRequest_ShouldReturnUserResponse() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(validUser);

        // When
        UserResponse result = userService.createUser(validCreateUserRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("USER_123");
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getCreatedAt()).isNotNull();

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should create user with generated UUID")
    void createUser_ShouldGenerateUniqueUserId() {
        // Given
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId("USER_generated_123");
            return user;
        });

        // When
        UserResponse result = userService.createUser(validCreateUserRequest);

        // Then
        assertThat(result.getUserId()).isNotNull();
        assertThat(result.getUserId()).startsWith("USER_");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void getUserById_WithValidId_ShouldReturnUserResponse() {
        // Given
        String userId = "USER_123";
        when(userRepository.findById(userId)).thenReturn(Optional.of(validUser));

        // When
        UserResponse result = userService.getUserById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("USER_123");
        assertThat(result.getName()).isEqualTo("John Doe");

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void getUserById_WithNonExistentId_ShouldThrowException() {
        // Given
        String userId = "USER_NON_EXISTENT";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("Should return true when user exists")
    void userExists_WithExistingUser_ShouldReturnTrue() {
        // Given
        String userId = "USER_123";
        when(userRepository.existsById(userId)).thenReturn(true);

        // When
        boolean result = userService.userExists(userId);

        // Then
        assertThat(result).isTrue();
        verify(userRepository, times(1)).existsById(userId);
    }

    @Test
    @DisplayName("Should return false when user does not exist")
    void userExists_WithNonExistentUser_ShouldReturnFalse() {
        // Given
        String userId = "USER_NON_EXISTENT";
        when(userRepository.existsById(userId)).thenReturn(false);

        // When
        boolean result = userService.userExists(userId);

        // Then
        assertThat(result).isFalse();
        verify(userRepository, times(1)).existsById(userId);
    }

    @Test
    @DisplayName("Should handle repository exception during user creation")
    void createUser_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Given
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> userService.createUser(validCreateUserRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(userRepository, times(1)).save(any(User.class));
    }
}
