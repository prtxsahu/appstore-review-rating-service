package com.example.appstore.app.service;

import com.example.appstore.app.domain.App;
import com.example.appstore.app.dto.AppResponse;
import com.example.appstore.app.dto.CreateAppRequest;
import com.example.appstore.app.repository.dynamodb.AppRepository;
import com.example.appstore.app.repository.elasticsearch.AppSearchRepository;
import com.example.appstore.comment.dto.CommentResponse;
import com.example.appstore.comment.service.CommentServiceInterface;
import com.example.appstore.rating.domain.Aggregate;
import com.example.appstore.rating.repository.dynamodb.AggregateRepository;
import com.example.appstore.rating.service.RatingServiceInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Simple unit tests for AppService.
 * Tests the business logic for app management operations with dual-write pattern.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppService Simple Tests")
class AppServiceSimpleTest {

    @Mock
    private AppRepository appRepository;

    @Mock
    private AggregateRepository aggregateRepository;

    @Mock
    private AppSearchRepository appSearchRepository;

    @Mock
    private CommentServiceInterface commentService;

    @Mock
    private RatingServiceInterface ratingService;

    @InjectMocks
    private AppService appService;

    private CreateAppRequest validCreateAppRequest;
    private App validApp;
    private AppResponse expectedAppResponse;
    private Aggregate validAggregate;

    @BeforeEach
    void setUp() {
        // Setup test data
        validCreateAppRequest = CreateAppRequest.builder()
                .name("Test App")
                .description("A test application")
                .build();

        validApp = App.builder()
                .appId("APP_123")
                .name("Test App")
                .description("A test application")
                .avgRating(BigDecimal.ZERO)
                .updatedAt(Instant.now())
                .build();

        expectedAppResponse = AppResponse.builder()
                .appId("APP_123")
                .name("Test App")
                .description("A test application")
                .avgRating(BigDecimal.ZERO)
                .updatedAt(validApp.getUpdatedAt())
                .build();

        validAggregate = Aggregate.builder()
                .appId("APP_123")
                .totalSum(40L)
                .totalCount(10L)
                .lastUpdated(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should create app successfully with dual-write")
    void createApp_WithValidRequest_ShouldReturnAppResponse() {
        // Given
        when(appRepository.save(any(App.class))).thenReturn(validApp);
        doNothing().when(appSearchRepository).indexApp(any(App.class));

        // When
        AppResponse result = appService.createApp(validCreateAppRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAppId()).isEqualTo("APP_123");
        assertThat(result.getName()).isEqualTo("Test App");
        assertThat(result.getDescription()).isEqualTo("A test application");
        assertThat(result.getAvgRating()).isEqualTo(BigDecimal.ZERO);

        verify(appRepository, times(1)).save(any(App.class));
        verify(appSearchRepository, times(1)).indexApp(any(App.class));
    }

    @Test
    @DisplayName("Should create app with generated UUID")
    void createApp_ShouldGenerateUniqueAppId() {
        // Given
        when(appRepository.save(any(App.class))).thenAnswer(invocation -> {
            App app = invocation.getArgument(0);
            app.setAppId("APP_generated_123");
            return app;
        });
        doNothing().when(appSearchRepository).indexApp(any(App.class));

        // When
        AppResponse result = appService.createApp(validCreateAppRequest);

        // Then
        assertThat(result.getAppId()).isNotNull();
        assertThat(result.getAppId()).startsWith("APP_");
        verify(appRepository, times(1)).save(any(App.class));
        verify(appSearchRepository, times(1)).indexApp(any(App.class));
    }

    @Test
    @DisplayName("Should get app by ID successfully")
    void getAppById_WithValidId_ShouldReturnAppResponse() {
        // Given
        String appId = "APP_123";
        when(appRepository.findById(appId)).thenReturn(Optional.of(validApp));
        when(aggregateRepository.findByAppId(appId)).thenReturn(Optional.of(validAggregate));

        // When
        AppResponse result = appService.getAppById(appId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAppId()).isEqualTo("APP_123");
        assertThat(result.getName()).isEqualTo("Test App");
        assertThat(result.getAvgRating()).isEqualTo(new BigDecimal("4.00")); // 40/10 = 4.00

        verify(appRepository, times(1)).findById(appId);
        verify(aggregateRepository, times(1)).findByAppId(appId);
    }

    @Test
    @DisplayName("Should get app by ID with zero rating when no aggregate exists")
    void getAppById_WithNoAggregate_ShouldReturnZeroRating() {
        // Given
        String appId = "APP_123";
        when(appRepository.findById(appId)).thenReturn(Optional.of(validApp));
        when(aggregateRepository.findByAppId(appId)).thenReturn(Optional.empty());

        // When
        AppResponse result = appService.getAppById(appId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAppId()).isEqualTo("APP_123");
        assertThat(result.getAvgRating()).isEqualTo(BigDecimal.ZERO);

        verify(appRepository, times(1)).findById(appId);
        verify(aggregateRepository, times(1)).findByAppId(appId);
    }

    @Test
    @DisplayName("Should throw exception when app not found")
    void getAppById_WithNonExistentId_ShouldThrowException() {
        // Given
        String appId = "APP_NON_EXISTENT";
        when(appRepository.findById(appId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> appService.getAppById(appId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("App not found");

        verify(appRepository, times(1)).findById(appId);
    }

    @Test
    @DisplayName("Should get app by ID with user-specific data")
    void getAppById_WithUserId_ShouldReturnAppWithUserData() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        when(appRepository.findById(appId)).thenReturn(Optional.of(validApp));
        when(aggregateRepository.findByAppId(appId)).thenReturn(Optional.of(validAggregate));
        when(ratingService.getUserRatingForApp(appId, userId)).thenReturn(5);
        when(commentService.getCommentsByUserForApp(appId, userId)).thenReturn(Arrays.asList(
                CommentResponse.builder().commentId("COMMENT_1").build()
        ));

        // When
        AppResponse result = appService.getAppById(appId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAppId()).isEqualTo("APP_123");
        assertThat(result.getUserRating()).isEqualTo(5);
        assertThat(result.getUserComments()).hasSize(1);

        verify(appRepository, times(1)).findById(appId);
        verify(aggregateRepository, times(1)).findByAppId(appId);
        verify(ratingService, times(1)).getUserRatingForApp(appId, userId);
        verify(commentService, times(1)).getCommentsByUserForApp(appId, userId);
    }

    @Test
    @DisplayName("Should search apps successfully")
    void searchApps_WithValidQuery_ShouldReturnMatchingApps() {
        // Given
        String query = "test";
        int page = 0;
        int size = 10;
        List<App> searchResults = Arrays.asList(validApp);
        when(appSearchRepository.searchApps(query, page, size)).thenReturn(searchResults);

        // When
        List<AppResponse> result = appService.searchApps(query, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAppId()).isEqualTo("APP_123");
        assertThat(result.get(0).getName()).isEqualTo("Test App");

        verify(appSearchRepository, times(1)).searchApps(query, page, size);
        verify(aggregateRepository, never()).findByAppId(anyString());
    }

    @Test
    @DisplayName("Should return true when app exists")
    void appExists_WithExistingApp_ShouldReturnTrue() {
        // Given
        String appId = "APP_123";
        when(appRepository.existsById(appId)).thenReturn(true);

        // When
        boolean result = appService.appExists(appId);

        // Then
        assertThat(result).isTrue();
        verify(appRepository, times(1)).existsById(appId);
    }

    @Test
    @DisplayName("Should return false when app does not exist")
    void appExists_WithNonExistentApp_ShouldReturnFalse() {
        // Given
        String appId = "APP_NON_EXISTENT";
        when(appRepository.existsById(appId)).thenReturn(false);

        // When
        boolean result = appService.appExists(appId);

        // Then
        assertThat(result).isFalse();
        verify(appRepository, times(1)).existsById(appId);
    }

    @Test
    @DisplayName("Should handle repository exception during app creation")
    void createApp_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Given
        when(appRepository.save(any(App.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> appService.createApp(validCreateAppRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(appRepository, times(1)).save(any(App.class));
    }

    @Test
    @DisplayName("Should handle search repository exception")
    void searchApps_WhenSearchRepositoryThrowsException_ShouldPropagateException() {
        // Given
        String query = "test";
        when(appSearchRepository.searchApps(anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Search error"));

        // When & Then
        assertThatThrownBy(() -> appService.searchApps(query, 0, 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Search error");

        verify(appSearchRepository, times(1)).searchApps(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should handle aggregate repository exception during app retrieval")
    void getAppById_WhenAggregateRepositoryThrowsException_ShouldPropagateException() {
        // Given
        String appId = "APP_123";
        when(appRepository.findById(appId)).thenReturn(Optional.of(validApp));
        when(aggregateRepository.findByAppId(appId)).thenThrow(new RuntimeException("Aggregate error"));

        // When & Then
        assertThatThrownBy(() -> appService.getAppById(appId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Aggregate error");

        verify(appRepository, times(1)).findById(appId);
        verify(aggregateRepository, times(1)).findByAppId(appId);
    }

    @Test
    @DisplayName("Should handle user rating service exception")
    void getAppById_WithUserId_WhenRatingServiceThrowsException_ShouldPropagateException() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        when(appRepository.findById(appId)).thenReturn(Optional.of(validApp));
        when(aggregateRepository.findByAppId(appId)).thenReturn(Optional.of(validAggregate));
        when(ratingService.getUserRatingForApp(appId, userId)).thenThrow(new RuntimeException("Rating service error"));

        // When & Then
        assertThatThrownBy(() -> appService.getAppById(appId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rating service error");

        verify(appRepository, times(1)).findById(appId);
        verify(aggregateRepository, times(1)).findByAppId(appId);
        verify(ratingService, times(1)).getUserRatingForApp(appId, userId);
    }
}
