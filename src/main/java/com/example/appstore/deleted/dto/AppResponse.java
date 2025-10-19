package com.example.appstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for app data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppResponse {

    private String appId;
    private String name;
    private String description;
    private BigDecimal avgRating;
    private Instant updatedAt;
    
    // For GET /apps/{appId} endpoint - includes top-level comments
    private List<CommentResponse> comments;
    private CommentPaginationInfo commentPagination;
}
