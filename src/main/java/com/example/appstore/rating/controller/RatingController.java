package com.example.appstore.rating.controller;

import com.example.appstore.rating.dto.RatingRequest;
import com.example.appstore.rating.dto.RatingResponse;
import com.example.appstore.rating.service.RatingServiceInterface;
import com.example.appstore.rating.domain.Rating;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for rating-related operations.
 * Currently returns dummy responses - will be connected to service layer later.
 */
@Slf4j
@RestController
@RequestMapping("/apps/{appId}/rating")
@RequiredArgsConstructor
public class RatingController {
    
    private final RatingServiceInterface ratingService;

    /**
     * Create a new rating for an app.
     * POST /apps/{appId}/rating
     */
    @PostMapping
    public ResponseEntity<RatingResponse> createRating(
            @PathVariable String appId,
            @Valid @RequestBody RatingRequest request) {

        log.info("Creating rating for appId: {}, userId: {}, value: {}",
                appId, request.getUserId(), request.getValue());

        // Check if rating already exists
        if (ratingService.ratingExists(appId, request.getUserId())) {
            log.warn("Rating already exists for appId: {}, userId: {}", appId, request.getUserId());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Create new rating
        Rating rating = ratingService.createRating(appId, request.getUserId(), request.getValue());
        log.info("Rating created successfully - appId: {}, userId: {}", appId, request.getUserId());

        // Convert to response DTO
        RatingResponse response = RatingResponse.builder()
                .appId(rating.getAppId())
                .userId(rating.getUserId())
                .value(rating.getValue())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing rating for an app.
     * PUT /apps/{appId}/rating
     */
    @PutMapping
    public ResponseEntity<RatingResponse> updateRating(
            @PathVariable String appId,
            @Valid @RequestBody RatingRequest request) {

        log.info("Updating rating for appId: {}, userId: {}, value: {}",
                appId, request.getUserId(), request.getValue());

        // Check if rating exists
        if (!ratingService.ratingExists(appId, request.getUserId())) {
            log.warn("Rating not found for appId: {}, userId: {}", appId, request.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Update existing rating
        Rating rating = ratingService.updateRating(appId, request.getUserId(), request.getValue());
        log.info("Rating updated successfully - appId: {}, userId: {}", appId, request.getUserId());

        // Convert to response DTO
        RatingResponse response = RatingResponse.builder()
                .appId(rating.getAppId())
                .userId(rating.getUserId())
                .value(rating.getValue())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a rating for an app.
     * DELETE /apps/{appId}/rating?userId={userId}
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteRating(
            @PathVariable String appId,
            @RequestParam String userId) {
        
        log.info("Deleting rating for appId: {}, userId: {}", appId, userId);
        
        ratingService.deleteRating(appId, userId);
        
        log.info("Rating deleted successfully - appId: {}, userId: {}", appId, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get a rating for an app and user.
     * GET /apps/{appId}/rating?userId={userId}
     */
    @GetMapping
    public ResponseEntity<RatingResponse> getRating(
            @PathVariable String appId,
            @RequestParam String userId) {
        
        log.info("Getting rating for appId: {}, userId: {}", appId, userId);
        
        Optional<Rating> ratingOpt = ratingService.getRating(appId, userId);
        
        if (ratingOpt.isEmpty()) {
            log.info("Rating not found for appId: {}, userId: {}", appId, userId);
            return ResponseEntity.notFound().build();
        }
        
        Rating rating = ratingOpt.get();
        RatingResponse response = RatingResponse.builder()
                .appId(rating.getAppId())
                .userId(rating.getUserId())
                .value(rating.getValue())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
        
        log.info("Rating found for appId: {}, userId: {}", appId, userId);
        return ResponseEntity.ok(response);
    }
}
