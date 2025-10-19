package com.example.appstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for paginated comments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentPageResponse {
    
    private List<CommentResponse> comments;
    private int page;
    private int size;
    private boolean hasNext;
    
    // Note: We're not including totalElements/totalPages for performance
    // as per user's requirement to skip total count
}
