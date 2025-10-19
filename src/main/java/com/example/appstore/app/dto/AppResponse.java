package com.example.appstore.app.dto;

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
    private Integer userRating;
    // User-specific comments for the requesting user
    private List<com.example.appstore.comment.dto.CommentResponse> userComments;
    
    // For GET /apps/{appId} endpoint - includes top-level comments
    private List<com.example.appstore.comment.dto.CommentResponse> comments;
    private com.example.appstore.comment.dto.CommentPaginationInfo commentPagination;
}
