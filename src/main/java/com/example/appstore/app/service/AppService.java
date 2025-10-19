package com.example.appstore.app.service;

import com.example.appstore.app.domain.App;
import com.example.appstore.app.dto.AppResponse;
import com.example.appstore.app.dto.CreateAppRequest;
import com.example.appstore.app.repository.dynamodb.AppRepository;
import com.example.appstore.app.repository.elasticsearch.AppSearchRepository;
import com.example.appstore.rating.repository.dynamodb.AggregateRepository;
import com.example.appstore.rating.service.RatingServiceInterface;
import com.example.appstore.comment.service.CommentServiceInterface;
import com.example.appstore.comment.dto.CommentResponse;
import com.example.appstore.comment.dto.CommentPaginationInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for App operations.
 * Handles business logic for app management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppService implements AppServiceInterface {

    private final AppRepository appRepository;
    private final AggregateRepository aggregateRepository;
    private final AppSearchRepository appSearchRepository;
    private final CommentServiceInterface commentService;
    private final RatingServiceInterface ratingService;

    /**
     * Create a new app.
     */
    public AppResponse createApp(CreateAppRequest request) {
        log.info("=== APP SERVICE: Creating app ===");
        log.info("Input - name: {}, description: {}", request.getName(), request.getDescription());
        log.debug("Full request: {}", request);
        
        try {
            // Generate unique app ID
            String appId = "APP_" + UUID.randomUUID().toString();
            log.debug("Generated appId: {}", appId);
            
            // Create app entity
            App app = App.builder()
                    .appId(appId)
                    .name(request.getName())
                    .description(request.getDescription())
                    .avgRating(BigDecimal.ZERO) // New apps start with 0 rating
                    .updatedAt(Instant.now())
                    .build();
            log.debug("Created app entity: {}", app);
            
            // Save to DynamoDB
            log.info("Saving app to DynamoDB - appId: {}", appId);
            App savedApp = appRepository.save(app);
            log.info("App saved to DynamoDB successfully - appId: {}", savedApp.getAppId());
            log.debug("Saved app entity: {}", savedApp);
            
            // Save to Elasticsearch for search functionality
            log.info("Indexing app to Elasticsearch - appId: {}", appId);
            try {
                appSearchRepository.indexApp(savedApp);
                log.info("App indexed to Elasticsearch successfully - appId: {}", savedApp.getAppId());
            } catch (Exception e) {
                log.error("Failed to index app to Elasticsearch - appId: {}, error: {}", savedApp.getAppId(), e.getMessage(), e);
                // Note: We continue execution even if Elasticsearch indexing fails
                // This ensures DynamoDB remains the source of truth
            }
            
            // Convert to response DTO
            AppResponse response = AppResponse.builder()
                    .appId(savedApp.getAppId())
                    .name(savedApp.getName())
                    .description(savedApp.getDescription())
                    .avgRating(savedApp.getAvgRating())
                    .updatedAt(savedApp.getUpdatedAt())
                    .build();
            
            log.info("App creation completed successfully - appId: {}, name: {}", response.getAppId(), response.getName());
            log.debug("Response DTO: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to create app - name: {}, error: {}", request.getName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get app by ID.
     */
    public AppResponse getAppById(String appId) {
        return getAppById(appId, null);
    }
    
    /**
     * Get app by ID with optional user rating.
     */
    public AppResponse getAppById(String appId, String userId) {
        log.info("=== APP SERVICE: Getting app by ID ===");
        log.info("Input - appId: {}", appId);
        
        try {
            log.info("Fetching app from DynamoDB - appId: {}", appId);
            App app = appRepository.findById(appId)
                    .orElseThrow(() -> {
                        log.error("App not found in DynamoDB - appId: {}", appId);
                        return new RuntimeException("App not found: " + appId);
                    });
            log.info("App found in DynamoDB - appId: {}, name: {}", app.getAppId(), app.getName());
            log.debug("Retrieved app entity: {}", app);
            
            // Fetch first 10 comments for the app (excluding user's own comments if userId provided)
            List<CommentResponse> comments;
            if (userId != null && !userId.trim().isEmpty()) {
                log.info("Fetching first 10 comments for appId: {} excluding user: {}", appId, userId);
                comments = commentService.getTopLevelCommentsByAppIdExcludingUser(appId, userId, 0, 10);
                log.info("Found {} comments for appId: {} excluding user: {}", comments.size(), appId, userId);
            } else {
                log.info("Fetching first 10 comments for appId: {}", appId);
                comments = commentService.getTopLevelCommentsByAppId(appId, 0, 10);
                log.info("Found {} comments for appId: {}", comments.size(), appId);
            }
            
            // Determine if there are more comments (hasNext)
            boolean hasNext = comments.size() == 10; // If we got exactly 10, there might be more
            
            // Fetch average rating from aggregates table
            log.info("Fetching average rating from aggregates table for appId: {}", appId);
            BigDecimal avgRating = fetchAverageRatingFromAggregates(appId);
            log.info("Retrieved average rating: {} for appId: {}", avgRating, appId);
            
            // Fetch user rating if userId is provided
            Integer userRating = null;
            if (userId != null && !userId.trim().isEmpty()) {
                log.info("Fetching user rating for appId: {}, userId: {}", appId, userId);
                userRating = ratingService.getUserRatingForApp(appId, userId);
                log.info("Retrieved user rating: {} for appId: {}, userId: {}", userRating, appId, userId);
            }
            
            // Fetch user comments if userId is provided
            List<CommentResponse> userComments = new ArrayList<>();
            if (userId != null && !userId.trim().isEmpty()) {
                log.info("Fetching user comments for appId: {}, userId: {}", appId, userId);
                userComments = commentService.getCommentsByUserForApp(appId, userId);
                log.info("Retrieved {} user comments for appId: {}, userId: {}", userComments.size(), appId, userId);
            }
            
            // Create pagination info
            CommentPaginationInfo commentPagination = CommentPaginationInfo.builder()
                    .page(0)
                    .size(10)
                    .hasNext(hasNext)
                    .build();
            
            // Convert to response DTO
            AppResponse response = AppResponse.builder()
                    .appId(app.getAppId())
                    .name(app.getName())
                    .description(app.getDescription())
                    .avgRating(avgRating)
                    .userRating(userRating)
                    .updatedAt(app.getUpdatedAt())
                    .comments(comments)
                    .commentPagination(commentPagination)
                    .userComments(userComments)
                    .build();
            
            log.info("App retrieval completed successfully - appId: {}, name: {}", response.getAppId(), response.getName());
            log.debug("Response DTO: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to get app - appId: {}, error: {}", appId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Search apps by text.
     */
    public List<AppResponse> searchApps(String query, int page, int size) {
        log.info("=== APP SERVICE: Searching apps ===");
        log.info("Input - query: {}, page: {}, size: {}", query, page, size);
        
        try {
            // Search in Elasticsearch
            List<App> apps = appSearchRepository.searchApps(query, page, size);
            log.debug("Elasticsearch search response: {} apps found", apps.size());
            
            // Convert App domain objects to AppResponse DTOs
            List<AppResponse> results = new ArrayList<>();
            for (App app : apps) {
                AppResponse appResponse = AppResponse.builder()
                        .appId(app.getAppId())
                        .name(app.getName())
                        .description(app.getDescription())
                        .avgRating(app.getAvgRating())
                        .updatedAt(app.getUpdatedAt())
                        .build();
                
                results.add(appResponse);
            }
            
            log.info("Search completed successfully - query: {}, found: {} results", query, results.size());
            log.debug("Search results: {}", results);
            return results;
        } catch (Exception e) {
            log.error("Failed to search apps - query: {}, error: {}", query, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check if app exists.
     */
    public boolean appExists(String appId) {
        log.info("=== APP SERVICE: Checking if app exists ===");
        log.info("Input - appId: {}", appId);
        
        try {
            boolean exists = appRepository.existsById(appId);
            log.info("App existence check completed - appId: {}, exists: {}", appId, exists);
            return exists;
        } catch (Exception e) {
            log.error("Failed to check app existence - appId: {}, error: {}", appId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Fetch average rating from aggregates table.
     * Calculates average rating from totalSum and totalCount.
     */
    private BigDecimal fetchAverageRatingFromAggregates(String appId) {
        try {
            log.debug("Fetching aggregate data for appId: {}", appId);
            
            // Try to find aggregate data for the app
            var aggregateOpt = aggregateRepository.findByAppId(appId);
            
            if (aggregateOpt.isPresent()) {
                var aggregate = aggregateOpt.get();
                log.debug("Found aggregate data - appId: {}, totalSum: {}, totalCount: {}", 
                        appId, aggregate.getTotalSum(), aggregate.getTotalCount());
                
                // Calculate average rating
                if (aggregate.getTotalCount() != null && aggregate.getTotalCount() > 0) {
                    BigDecimal average = BigDecimal.valueOf(aggregate.getTotalSum())
                            .divide(BigDecimal.valueOf(aggregate.getTotalCount()), 2, RoundingMode.HALF_UP);
                    log.debug("Calculated average rating: {} for appId: {}", average, appId);
                    return average;
                } else {
                    log.warn("Total count is 0 or null for appId: {}, returning 0.00", appId);
                    return BigDecimal.ZERO;
                }
            } else {
                log.warn("No aggregate data found for appId: {}, returning 0.00", appId);
                return BigDecimal.ZERO;
            }
            
        } catch (Exception e) {
            log.error("Error fetching average rating from aggregates for appId: {}, error: {}", 
                    appId, e.getMessage(), e);
            // Return 0.00 as fallback if there's an error
            return BigDecimal.ZERO;
        }
    }
}
