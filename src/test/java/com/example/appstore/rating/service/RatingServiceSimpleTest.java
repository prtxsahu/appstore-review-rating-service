package com.example.appstore.rating.service;

import com.example.appstore.rating.domain.Rating;
import com.example.appstore.rating.repository.dynamodb.RatingRepository;
import com.example.appstore.rating.stream.RatingObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Simple unit tests for RatingService.
 * Tests the core business logic for rating management operations and observer pattern.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RatingService Simple Tests")
class RatingServiceSimpleTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingObserver mockObserver;

    @InjectMocks
    private RatingService ratingService;

    private Rating validRating;
    private Rating updatedRating;

    @BeforeEach
    void setUp() {
        // Setup test data
        validRating = Rating.builder()
                .appId("APP_123")
                .userId("USER_123")
                .value(5)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        updatedRating = Rating.builder()
                .appId("APP_123")
                .userId("USER_123")
                .value(4)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should create rating successfully")
    void createRating_WithValidData_ShouldReturnRating() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        Integer ratingValue = 5;
        when(ratingRepository.save(any(Rating.class))).thenReturn(validRating);

        // When
        Rating result = ratingService.createRating(appId, userId, ratingValue);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAppId()).isEqualTo("APP_123");
        assertThat(result.getUserId()).isEqualTo("USER_123");
        assertThat(result.getValue()).isEqualTo(5);

        verify(ratingRepository, times(1)).save(any(Rating.class));
    }

    @Test
    @DisplayName("Should update existing rating successfully")
    void updateRating_WithExistingRating_ShouldReturnUpdatedRating() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        Integer newRatingValue = 4;
        when(ratingRepository.findByAppIdAndUserId(appId, userId)).thenReturn(Optional.of(validRating));
        when(ratingRepository.updateRatingValue(appId, userId, newRatingValue)).thenReturn(newRatingValue);

        // When
        Rating result = ratingService.updateRating(appId, userId, newRatingValue);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAppId()).isEqualTo("APP_123");
        assertThat(result.getUserId()).isEqualTo("USER_123");
        assertThat(result.getValue()).isEqualTo(4);

        verify(ratingRepository, times(1)).findByAppIdAndUserId(appId, userId);
        verify(ratingRepository, times(1)).updateRatingValue(appId, userId, newRatingValue);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent rating")
    void updateRating_WithNonExistentRating_ShouldThrowException() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        Integer newRatingValue = 4;
        when(ratingRepository.findByAppIdAndUserId(appId, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ratingService.updateRating(appId, userId, newRatingValue))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rating not found");

        verify(ratingRepository, times(1)).findByAppIdAndUserId(appId, userId);
        verify(ratingRepository, never()).updateRatingValue(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Should delete rating successfully")
    void deleteRating_WithExistingRating_ShouldDeleteRating() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        when(ratingRepository.findByAppIdAndUserId(appId, userId)).thenReturn(Optional.of(validRating));
        doNothing().when(ratingRepository).deleteByAppIdAndUserId(appId, userId);

        // When
        ratingService.deleteRating(appId, userId);

        // Then
        verify(ratingRepository, times(1)).findByAppIdAndUserId(appId, userId);
        verify(ratingRepository, times(1)).deleteByAppIdAndUserId(appId, userId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent rating")
    void deleteRating_WithNonExistentRating_ShouldThrowException() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        when(ratingRepository.findByAppIdAndUserId(appId, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> ratingService.deleteRating(appId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rating not found");

        verify(ratingRepository, times(1)).findByAppIdAndUserId(appId, userId);
        verify(ratingRepository, never()).deleteByAppIdAndUserId(anyString(), anyString());
    }

    @Test
    @DisplayName("Should get rating by app ID and user ID")
    void getRating_WithValidIds_ShouldReturnRating() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        when(ratingRepository.findByAppIdAndUserId(appId, userId)).thenReturn(Optional.of(validRating));

        // When
        Optional<Rating> result = ratingService.getRating(appId, userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getAppId()).isEqualTo("APP_123");
        assertThat(result.get().getUserId()).isEqualTo("USER_123");
        assertThat(result.get().getValue()).isEqualTo(5);

        verify(ratingRepository, times(1)).findByAppIdAndUserId(appId, userId);
    }

    @Test
    @DisplayName("Should return empty when rating not found")
    void getRating_WithNonExistentRating_ShouldReturnEmpty() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        when(ratingRepository.findByAppIdAndUserId(appId, userId)).thenReturn(Optional.empty());

        // When
        Optional<Rating> result = ratingService.getRating(appId, userId);

        // Then
        assertThat(result).isEmpty();
        verify(ratingRepository, times(1)).findByAppIdAndUserId(appId, userId);
    }

    @Test
    @DisplayName("Should get all ratings for an app")
    void getRatingsForApp_WithValidAppId_ShouldReturnRatings() {
        // Given
        String appId = "APP_123";
        List<Rating> ratings = Arrays.asList(validRating);
        when(ratingRepository.findByAppId(appId)).thenReturn(ratings);

        // When
        List<Rating> result = ratingService.getRatingsForApp(appId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAppId()).isEqualTo("APP_123");

        verify(ratingRepository, times(1)).findByAppId(appId);
    }

    @Test
    @DisplayName("Should get all ratings for a user")
    void getRatingsForUser_WithValidUserId_ShouldReturnRatings() {
        // Given
        String userId = "USER_123";
        List<Rating> ratings = Arrays.asList(validRating);
        when(ratingRepository.findByUserId(userId)).thenReturn(ratings);

        // When
        List<Rating> result = ratingService.getRatingsForUser(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("USER_123");

        verify(ratingRepository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("Should return true when rating exists")
    void ratingExists_WithExistingRating_ShouldReturnTrue() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        when(ratingRepository.existsByAppIdAndUserId(appId, userId)).thenReturn(true);

        // When
        boolean result = ratingService.ratingExists(appId, userId);

        // Then
        assertThat(result).isTrue();
        verify(ratingRepository, times(1)).existsByAppIdAndUserId(appId, userId);
    }

    @Test
    @DisplayName("Should return false when rating does not exist")
    void ratingExists_WithNonExistentRating_ShouldReturnFalse() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        when(ratingRepository.existsByAppIdAndUserId(appId, userId)).thenReturn(false);

        // When
        boolean result = ratingService.ratingExists(appId, userId);

        // Then
        assertThat(result).isFalse();
        verify(ratingRepository, times(1)).existsByAppIdAndUserId(appId, userId);
    }

    @Test
    @DisplayName("Should get rating count for app")
    void getRatingCountForApp_WithValidAppId_ShouldReturnCount() {
        // Given
        String appId = "APP_123";
        when(ratingRepository.countByAppId(appId)).thenReturn(5L);

        // When
        long result = ratingService.getRatingCountForApp(appId);

        // Then
        assertThat(result).isEqualTo(5L);
        verify(ratingRepository, times(1)).countByAppId(appId);
    }

    @Test
    @DisplayName("Should get user rating for app")
    void getUserRatingForApp_WithValidIds_ShouldReturnRatingValue() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        when(ratingRepository.findByAppIdAndUserId(appId, userId)).thenReturn(Optional.of(validRating));

        // When
        Integer result = ratingService.getUserRatingForApp(appId, userId);

        // Then
        assertThat(result).isEqualTo(5);
        verify(ratingRepository, times(1)).findByAppIdAndUserId(appId, userId);
    }

    @Test
    @DisplayName("Should return null when user has no rating for app")
    void getUserRatingForApp_WithNoRating_ShouldReturnNull() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        when(ratingRepository.findByAppIdAndUserId(appId, userId)).thenReturn(Optional.empty());

        // When
        Integer result = ratingService.getUserRatingForApp(appId, userId);

        // Then
        assertThat(result).isNull();
        verify(ratingRepository, times(1)).findByAppIdAndUserId(appId, userId);
    }

    @Test
    @DisplayName("Should register observer successfully")
    void register_WithValidObserver_ShouldAddObserver() {
        // Given
        int initialCount = ratingService.getObserverCount();

        // When
        ratingService.register(mockObserver);

        // Then
        assertThat(ratingService.getObserverCount()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("Should unregister observer successfully")
    void unregister_WithValidObserver_ShouldRemoveObserver() {
        // Given
        ratingService.register(mockObserver);
        int countAfterRegister = ratingService.getObserverCount();

        // When
        ratingService.unregister(mockObserver);

        // Then
        assertThat(ratingService.getObserverCount()).isEqualTo(countAfterRegister - 1);
    }

    @Test
    @DisplayName("Should handle repository exception during rating creation")
    void createRating_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        Integer ratingValue = 5;
        when(ratingRepository.save(any(Rating.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> ratingService.createRating(appId, userId, ratingValue))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(ratingRepository, times(1)).save(any(Rating.class));
    }
}
