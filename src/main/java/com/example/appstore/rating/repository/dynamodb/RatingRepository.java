package com.example.appstore.rating.repository.dynamodb;

import com.example.appstore.rating.domain.Rating;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository for Rating entity operations using DynamoDB Enhanced Client.
 * Implements Repository pattern for data access abstraction.
 * Uses composite key: app_id (PK) + user_id (SK)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RatingRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String TABLE_NAME = "ratings";

    private DynamoDbTable<Rating> getRatingTable() {
        return dynamoDbEnhancedClient.table(TABLE_NAME, TableSchema.fromBean(Rating.class));
    }

    /**
     * Save a rating to DynamoDB.
     */
    public Rating save(Rating rating) {
        try {
            log.debug("Saving rating for app: {} and user: {}", rating.getAppId(), rating.getUserId());
            DynamoDbTable<Rating> ratingTable = getRatingTable();
            ratingTable.putItem(rating);
            log.info("Successfully saved rating for app: {} and user: {}", rating.getAppId(), rating.getUserId());
            return rating;
        } catch (DynamoDbException e) {
            log.error("Error saving rating for app: {} and user: {}", rating.getAppId(), rating.getUserId(), e);
            throw new RuntimeException("Failed to save rating", e);
        }
    }

    /**
     * Find a rating by app ID and user ID.
     */
    public Optional<Rating> findByAppIdAndUserId(String appId, String userId) {
        try {
            log.debug("Finding rating for app: {} and user: {}", appId, userId);
            DynamoDbTable<Rating> ratingTable = getRatingTable();
            Key key = Key.builder()
                    .partitionValue(appId)
                    .sortValue(userId)
                    .build();
            
            GetItemEnhancedRequest request = GetItemEnhancedRequest.builder()
                    .key(key)
                    .build();
            
            Rating rating = ratingTable.getItem(request);
            if (rating != null) {
                log.info("Found rating for app: {} and user: {}", appId, userId);
                return Optional.of(rating);
            } else {
                log.info("Rating not found for app: {} and user: {}", appId, userId);
                return Optional.empty();
            }
        } catch (DynamoDbException e) {
            log.error("Error finding rating for app: {} and user: {}", appId, userId, e);
            throw new RuntimeException("Failed to find rating", e);
        }
    }

    /**
     * Find all ratings for a specific app.
     */
    public List<Rating> findByAppId(String appId) {
        try {
            log.debug("Finding all ratings for app: {}", appId);
            DynamoDbTable<Rating> ratingTable = getRatingTable();
            Key key = Key.builder()
                    .partitionValue(appId)
                    .build();
            
            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                    .queryConditional(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo(key))
                    .build();
            
            return ratingTable.query(request)
                    .items()
                    .stream()
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            log.error("Error finding ratings for app: {}", appId, e);
            throw new RuntimeException("Failed to find ratings for app", e);
        }
    }

    /**
     * Find all ratings for a specific user.
     */
    public List<Rating> findByUserId(String userId) {
        try {
            log.debug("Finding all ratings for user: {}", userId);
            DynamoDbTable<Rating> ratingTable = getRatingTable();
            
            // Note: This requires a GSI on user_id for efficient querying
            // For now, we'll use scan (not recommended for production)
            return ratingTable.scan()
                    .items()
                    .stream()
                    .filter(rating -> userId.equals(rating.getUserId()))
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            log.error("Error finding ratings for user: {}", userId, e);
            throw new RuntimeException("Failed to find ratings for user", e);
        }
    }

    /**
     * Check if a rating exists for app and user.
     */
    public boolean existsByAppIdAndUserId(String appId, String userId) {
        return findByAppIdAndUserId(appId, userId).isPresent();
    }

    /**
     * Delete a rating by app ID and user ID.
     */
    public void deleteByAppIdAndUserId(String appId, String userId) {
        try {
            log.debug("Deleting rating for app: {} and user: {}", appId, userId);
            DynamoDbTable<Rating> ratingTable = getRatingTable();
            Key key = Key.builder()
                    .partitionValue(appId)
                    .sortValue(userId)
                    .build();
            
            ratingTable.deleteItem(key);
            log.info("Successfully deleted rating for app: {} and user: {}", appId, userId);
        } catch (DynamoDbException e) {
            log.error("Error deleting rating for app: {} and user: {}", appId, userId, e);
            throw new RuntimeException("Failed to delete rating", e);
        }
    }

    /**
     * Get count of ratings for an app.
     */
    public long countByAppId(String appId) {
        return findByAppId(appId).size();
    }
    
    /**
     * Update rating value efficiently.
     * Note: This currently uses save() but can be optimized with native DynamoDB update expressions later.
     * 
     * @param appId The application ID
     * @param userId The user ID
     * @param newValue The new rating value
     * @return The updated rating value
     */
    public Integer updateRatingValue(String appId, String userId, Integer newValue) {
        try {
            log.debug("Updating rating value for app: {} and user: {} to value: {}", appId, userId, newValue);
            
            // Find existing rating to preserve createdAt
            Optional<Rating> existingRating = findByAppIdAndUserId(appId, userId);
            if (existingRating.isEmpty()) {
                throw new RuntimeException("Rating not found for update");
            }
            
            // Update with new value and timestamp
            Rating updatedRating = Rating.builder()
                    .appId(appId)
                    .userId(userId)
                    .value(newValue)
                    .createdAt(existingRating.get().getCreatedAt())
                    .updatedAt(Instant.now())
                    .build();
            
            save(updatedRating);
            
            log.info("Successfully updated rating value for app: {} and user: {} to value: {}", appId, userId, newValue);
            return newValue;
            
        } catch (DynamoDbException e) {
            log.error("Error updating rating value for app: {} and user: {}", appId, userId, e);
            throw new RuntimeException("Failed to update rating value", e);
        }
    }
}
