package com.example.appstore.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pagination information for comments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentPaginationInfo {
    
    private int page;
    private int size;
    private boolean hasNext;
    
    // Note: We're not including totalElements/totalPages for performance
    // as per user's requirement to skip total count
}
