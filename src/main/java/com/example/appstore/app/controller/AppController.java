package com.example.appstore.app.controller;

import com.example.appstore.app.dto.*;
import com.example.appstore.app.service.AppServiceInterface;
import com.example.appstore.shared.dto.SearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for app-related operations.
 */
@Slf4j
@RestController
@RequestMapping("/apps")
@RequiredArgsConstructor
public class AppController {

    private final AppServiceInterface appService;

    /**
     * Create a new app.
     * POST /apps
     */
    @PostMapping
    public ResponseEntity<AppResponse> createApp(@Valid @RequestBody CreateAppRequest request) {
        log.info("=== APP CONTROLLER: Creating app ===");
        log.info("Request received - name: {}, description: {}", request.getName(), request.getDescription());
        log.debug("Full request: {}", request);
        
        try {
            AppResponse response = appService.createApp(request);
            log.info("App created successfully → appId: {}, name: {}", response.getAppId(), response.getName());
            log.debug("Full response: {}", response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Failed to create app - name: {}, error: {}", request.getName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get app by ID.
     * GET /apps/{appId}?userId={userId}
     */
    @GetMapping("/{appId}")
    public ResponseEntity<AppResponse> getApp(
            @PathVariable String appId,
            @RequestParam(required = false) String userId) {
        log.info("=== APP CONTROLLER: Getting app by ID ===");
        log.info("Request received - appId: {}, userId: {}", appId, userId);
        
        try {
            AppResponse response = appService.getAppById(appId, userId);
            log.info("App retrieved successfully - appId: {}, name: {}", response.getAppId(), response.getName());
            log.debug("Full response: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get app - appId: {}, error: {}", appId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Search apps by keyword.
     * GET /apps?keyword={keyword}&page={page}&size={size}
     */
    @GetMapping
    public ResponseEntity<SearchResponse<AppSearchResponse>> searchApps(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        
        log.info("=== APP CONTROLLER: Searching apps ===");
        log.info("Request received - keyword: {}, page: {}, size: {}", keyword, page, size);
        
        try {
            // Call AppService to search apps
            List<AppResponse> appResponses = appService.searchApps(keyword, page, size);
            
            // Convert AppResponse to AppSearchResponse
            List<AppSearchResponse> searchResults = appResponses.stream()
                    .map(app -> AppSearchResponse.builder()
                            .appId(app.getAppId())
                            .name(app.getName())
                            .description(app.getDescription())
                            .avgRating(app.getAvgRating())
                            .updatedAt(app.getUpdatedAt())
                            .build())
                    .toList();
            
            // Calculate pagination info
            long totalElements = searchResults.size();
            long totalPages = (totalElements + size - 1) / size;
            boolean hasNext = page < totalPages - 1;
            boolean hasPrevious = page > 0;
            
            SearchResponse<AppSearchResponse> response = SearchResponse.<AppSearchResponse>builder()
                    .items(searchResults)
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages((int) totalPages)
                    .hasNext(hasNext)
                    .hasPrevious(hasPrevious)
                    .build();
            
            log.info("Search completed successfully - keyword: {}, found: {} results", keyword, searchResults.size());
            log.debug("Search response: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to search apps - keyword: {}, error: {}", keyword, e.getMessage(), e);
            throw e;
        }
    }

}
