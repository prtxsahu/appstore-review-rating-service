// MOVED TO: com.example.appstore.rating.service.RatingService
// This file has been moved to the new feature-based structure
// TODO: Remove this file after confirming the refactoring is complete

/*
package com.example.appstore.service;

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
/*
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {
    
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
    /*
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
 Includes the rest of the methods...
    }
    
    // All other methods would be here...
}
*/