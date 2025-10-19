package com.example.appstore.app.service;

import com.example.appstore.app.dto.AppResponse;
import com.example.appstore.app.dto.CreateAppRequest;

import java.util.List;

/**
 * Interface for App service operations.
 * Defines the contract for app management business logic.
 */
public interface AppServiceInterface {
    
    /**
     * Create a new app.
     * 
     * @param request The app creation request
     * @return The created app response
     */
    AppResponse createApp(CreateAppRequest request);
    
    /**
     * Get an app by its ID.
     * 
     * @param appId The app ID
     * @return The app response
     */
    AppResponse getAppById(String appId);
    
    /**
     * Get an app by its ID with user-specific data.
     * 
     * @param appId The app ID
     * @param userId The user ID for user-specific data
     * @return The app response with user-specific data
     */
    AppResponse getAppById(String appId, String userId);
    
    /**
     * Search for apps by query.
     * 
     * @param query The search query
     * @param page The page number (0-based)
     * @param size The page size
     * @return List of matching app responses
     */
    List<AppResponse> searchApps(String query, int page, int size);
    
    /**
     * Check if an app exists.
     * 
     * @param appId The app ID to check
     * @return true if the app exists, false otherwise
     */
    boolean appExists(String appId);
}
