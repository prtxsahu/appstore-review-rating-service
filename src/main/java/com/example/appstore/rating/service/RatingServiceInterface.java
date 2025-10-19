package com.example.appstore.rating.service;

import com.example.appstore.rating.domain.Rating;
import com.example.appstore.rating.stream.RatingObserver;

import java.util.List;
import java.util.Optional;

/**
 * Interface for Rating service operations.
 * Defines the contract for rating management business logic.
 */
public interface RatingServiceInterface {
    
    /**
     * Create a new rating for an app.
     * 
     * @param appId The app ID
     * @param userId The user ID
     * @param ratingValue The rating value (1-5)
     * @return The created rating
     */
    Rating createRating(String appId, String userId, Integer ratingValue);
    
    /**
     * Update an existing rating.
     * 
     * @param appId The app ID
     * @param userId The user ID
     * @param newRatingValue The new rating value (1-5)
     * @return The updated rating
     */
    Rating updateRating(String appId, String userId, Integer newRatingValue);
    
    /**
     * Delete a rating.
     * 
     * @param appId The app ID
     * @param userId The user ID
     */
    void deleteRating(String appId, String userId);
    
    /**
     * Get a rating by app ID and user ID.
     * 
     * @param appId The app ID
     * @param userId The user ID
     * @return Optional containing the rating if found
     */
    Optional<Rating> getRating(String appId, String userId);
    
    /**
     * Get all ratings for an app.
     * 
     * @param appId The app ID
     * @return List of ratings for the app
     */
    List<Rating> getRatingsForApp(String appId);
    
    /**
     * Get all ratings by a user.
     * 
     * @param userId The user ID
     * @return List of ratings by the user
     */
    List<Rating> getRatingsForUser(String userId);
    
    /**
     * Check if a rating exists.
     * 
     * @param appId The app ID
     * @param userId The user ID
     * @return true if the rating exists, false otherwise
     */
    boolean ratingExists(String appId, String userId);
    
    /**
     * Get the count of ratings for an app.
     * 
     * @param appId The app ID
     * @return The number of ratings for the app
     */
    long getRatingCountForApp(String appId);
    
    /**
     * Get a user's rating for an app.
     * 
     * @param appId The app ID
     * @param userId The user ID
     * @return The user's rating value, or null if no rating exists
     */
    Integer getUserRatingForApp(String appId, String userId);
    
    /**
     * Register a rating observer.
     * 
     * @param observer The observer to register
     */
    void register(RatingObserver observer);
    
    /**
     * Unregister a rating observer.
     * 
     * @param observer The observer to unregister
     */
    void unregister(RatingObserver observer);
    
    /**
     * Get the count of registered observers.
     * 
     * @return The number of registered observers
     */
    int getObserverCount();
}
