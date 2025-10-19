package com.example.appstore.rating.service;

import com.example.appstore.rating.domain.Rating;
import com.example.appstore.rating.repository.dynamodb.RatingRepository;
import com.example.appstore.rating.stream.RatingObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing ratings and notifying observers.
 * Handles CRUD operations for ratings and maintains a list of observers
 * to notify when rating changes occur.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService implements RatingServiceInterface {
    
    private final RatingRepository ratingRepository;
    
    // List of observers to notify when rating changes occur
    private final List<RatingObserver> observers = new ArrayList<>();
    
    /**
     * Create a new rating.
     * 
     * @param appId The application ID
     * @param userId The user ID
     * @param ratingValue The rating value (1-5)
     * @return The created rating
     */
    @Override
    public Rating createRating(String appId, String userId, Integer ratingValue) {
        log.info("Creating rating - appId: {}, userId: {}, value: {}", appId, userId, ratingValue);
        
        // Validate rating value
        if (ratingValue < 1 || ratingValue > 5) {
            throw new IllegalArgumentException("Rating value must be between 1 and 5");
        }
        
        // Check if rating already exists
        if (ratingRepository.existsByAppIdAndUserId(appId, userId)) {
            throw new IllegalStateException("Rating already exists for this app and user");
        }
        
        // Create new rating
        Instant now = Instant.now();
        Rating rating = Rating.builder()
                .appId(appId)
                .userId(userId)
                .value(ratingValue)
                .createdAt(now)
                .updatedAt(now)
                .build();
        
        // Save to database
        Rating savedRating = ratingRepository.save(rating);
        
        // Notify observers
        notifyRatingCreated(appId, userId, ratingValue, now);
        
        log.info("Rating created successfully - appId: {}, userId: {}, value: {}", appId, userId, ratingValue);
        return savedRating;
    }
    
    /**
     * Update an existing rating.
     * 
     * @param appId The application ID
     * @param userId The user ID
     * @param newRatingValue The new rating value (1-5)
     * @return The updated rating
     */
    @Override
    public Rating updateRating(String appId, String userId, Integer newRatingValue) {
        log.info("Updating rating - appId: {}, userId: {}, newValue: {}", appId, userId, newRatingValue);
        
        // Validate rating value
        if (newRatingValue < 1 || newRatingValue > 5) {
            throw new IllegalArgumentException("Rating value must be between 1 and 5");
        }
        
        // Find existing rating to get old value for observer notification
        Optional<Rating> existingRatingOpt = ratingRepository.findByAppIdAndUserId(appId, userId);
        if (existingRatingOpt.isEmpty()) {
            throw new IllegalStateException("Rating not found for this app and user");
        }
        
        Rating existingRating = existingRatingOpt.get();
        Integer oldRatingValue = existingRating.getValue();
        
        // Update rating using efficient native DynamoDB update operation
        Integer updatedValue = ratingRepository.updateRatingValue(appId, userId, newRatingValue);
        
        // Notify observers
        Instant now = Instant.now();
        notifyRatingUpdated(appId, userId, oldRatingValue, newRatingValue, now);
        
        log.info("Rating updated successfully - appId: {}, userId: {}, oldValue: {}, newValue: {}", 
                appId, userId, oldRatingValue, newRatingValue);
        
        // Return the updated rating object for consistency with the interface
        Rating updatedRating = Rating.builder()
                .appId(existingRating.getAppId())
                .userId(existingRating.getUserId())
                .value(updatedValue)
                .createdAt(existingRating.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
        return updatedRating;
    }
    
    /**
     * Delete a rating.
     * 
     * @param appId The application ID
     * @param userId The user ID
     */
    @Override
    public void deleteRating(String appId, String userId) {
        log.info("Deleting rating - appId: {}, userId: {}", appId, userId);
        
        // Find existing rating
        Optional<Rating> existingRatingOpt = ratingRepository.findByAppIdAndUserId(appId, userId);
        if (existingRatingOpt.isEmpty()) {
            throw new IllegalStateException("Rating not found for this app and user");
        }
        
        Rating existingRating = existingRatingOpt.get();
        Integer deletedRatingValue = existingRating.getValue();
        
        // Delete from database
        ratingRepository.deleteByAppIdAndUserId(appId, userId);
        
        // Notify observers
        Instant now = Instant.now();
        notifyRatingDeleted(appId, userId, deletedRatingValue, now);
        
        log.info("Rating deleted successfully - appId: {}, userId: {}", appId, userId);
    }
    
    /**
     * Get a rating by app ID and user ID.
     * 
     * @param appId The application ID
     * @param userId The user ID
     * @return The rating if found, empty otherwise
     */
    @Override
    public Optional<Rating> getRating(String appId, String userId) {
        log.debug("Getting rating - appId: {}, userId: {}", appId, userId);
        return ratingRepository.findByAppIdAndUserId(appId, userId);
    }
    
    /**
     * Get all ratings for an app.
     * 
     * @param appId The application ID
     * @return List of ratings for the app
     */
    @Override
    public List<Rating> getRatingsForApp(String appId) {
        log.debug("Getting ratings for app - appId: {}", appId);
        return ratingRepository.findByAppId(appId);
    }
    
    /**
     * Get all ratings for a user.
     * 
     * @param userId The user ID
     * @return List of ratings by the user
     */
    @Override
    public List<Rating> getRatingsForUser(String userId) {
        log.debug("Getting ratings for user - userId: {}", userId);
        return ratingRepository.findByUserId(userId);
    }
    
    /**
     * Check if a rating exists.
     * 
     * @param appId The application ID
     * @param userId The user ID
     * @return true if rating exists, false otherwise
     */
    @Override
    public boolean ratingExists(String appId, String userId) {
        return ratingRepository.existsByAppIdAndUserId(appId, userId);
    }
    
    /**
     * Get count of ratings for an app.
     * 
     * @param appId The application ID
     * @return Number of ratings for the app
     */
    @Override
    public long getRatingCountForApp(String appId) {
        return ratingRepository.countByAppId(appId);
    }
    
    /**
     * Get a user's rating for a specific app.
     * 
     * @param appId The application ID
     * @param userId The user ID
     * @return The rating value if found, null if no rating exists
     */
    @Override
    public Integer getUserRatingForApp(String appId, String userId) {
        log.info("Getting user rating - appId: {}, userId: {}", appId, userId);
        
        try {
            Optional<Rating> ratingOpt = ratingRepository.findByAppIdAndUserId(appId, userId);
            
            if (ratingOpt.isPresent()) {
                Integer ratingValue = ratingOpt.get().getValue();
                log.info("Found user rating - appId: {}, userId: {}, rating: {}", appId, userId, ratingValue);
                return ratingValue;
            } else {
                log.info("No rating found for user - appId: {}, userId: {}", appId, userId);
                return null;
            }
        } catch (Exception e) {
            log.error("Error getting user rating - appId: {}, userId: {}, error: {}", appId, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user rating", e);
        }
    }
    
    // ==================== Observer Management ====================
    
    /**
     * Register an observer to receive rating change notifications.
     * 
     * @param observer The observer to register
     */
    @Override
    public void register(RatingObserver observer) {
        synchronized (observers) {
            if (!observers.contains(observer)) {
                observers.add(observer);
                log.info("Observer {} registered with RatingService", observer.getClass().getSimpleName());
            } else {
                log.debug("Observer {} already registered", observer.getClass().getSimpleName());
            }
        }
    }

    /**
     * Unregister an observer from receiving rating change notifications.
     * 
     * @param observer The observer to unregister
     */
    @Override
    public void unregister(RatingObserver observer) {
        synchronized (observers) {
            if (observers.remove(observer)) {
                log.info("Observer {} unregistered from RatingService", observer.getClass().getSimpleName());
            } else {
                log.debug("Observer {} was not registered", observer.getClass().getSimpleName());
            }
        }
    }
    
    /**
     * Get the number of subscribed observers.
     * 
     * @return Number of observers
     */
    @Override
    public int getObserverCount() {
        synchronized (observers) {
            return observers.size();
        }
    }
    
    // ==================== Observer Notification Methods ====================
    
    /**
     * Notify all observers about a rating creation.
     */
    private void notifyRatingCreated(String appId, String userId, Integer ratingValue, Instant timestamp) {
        synchronized (observers) {
            for (RatingObserver observer : observers) {
                try {
                    observer.onRatingCreated(appId, userId, ratingValue, timestamp);
                } catch (Exception e) {
                    log.error("Error notifying observer {} about rating creation", observer.getClass().getSimpleName(), e);
                    observer.onRatingError(appId, userId, e);
                }
            }
        }
    }
    
    /**
     * Notify all observers about a rating update.
     */
    private void notifyRatingUpdated(String appId, String userId, Integer oldRatingValue, Integer newRatingValue, Instant timestamp) {
        synchronized (observers) {
            for (RatingObserver observer : observers) {
                try {
                    observer.onRatingUpdated(appId, userId, oldRatingValue, newRatingValue, timestamp);
                } catch (Exception e) {
                    log.error("Error notifying observer {} about rating update", observer.getClass().getSimpleName(), e);
                    observer.onRatingError(appId, userId, e);
                }
            }
        }
    }
    
    /**
     * Notify all observers about a rating deletion.
     */
    private void notifyRatingDeleted(String appId, String userId, Integer deletedRatingValue, Instant timestamp) {
        synchronized (observers) {
            for (RatingObserver observer : observers) {
                try {
                    observer.onRatingDeleted(appId, userId, deletedRatingValue, timestamp);
                } catch (Exception e) {
                    log.error("Error notifying observer {} about rating deletion", observer.getClass().getSimpleName(), e);
                    observer.onRatingError(appId, userId, e);
                }
            }
        }
    }
}
