package com.example.appstore.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

/**
 * Generic paginated result wrapper for DynamoDB queries.
 * Provides cursor-based pagination using DynamoDB's lastEvaluatedKey.
 * 
 * @param <T> The type of items in the result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResult<T> {
    
    /**
     * The list of items for the current page
     */
    private List<T> items;
    
    /**
     * The last evaluated key from DynamoDB for cursor-based pagination.
     * This can be used as the cursor for the next page.
     * If null, there are no more pages.
     * 
     * Note: This field is excluded from JSON serialization because AttributeValue
     * objects cannot be serialized by Jackson. Use getNextPageCursor() instead.
     */
    @JsonIgnore
    private Map<String, AttributeValue> lastEvaluatedKey;
    
    /**
     * Whether there are more pages available.
     * True if lastEvaluatedKey is not null and not empty.
     */
    public boolean hasNextPage() {
        return lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty();
    }
    
    /**
     * Whether there are more pages available (for JSON serialization).
     * This is a convenience method that returns the same as hasNextPage().
     */
    public boolean getHasNextPage() {
        return hasNextPage();
    }
    
    /**
     * Get the cursor for the next page.
     * Returns null if there are no more pages.
     */
    public String getNextPageCursor() {
        if (!hasNextPage()) {
            return null;
        }
        
        // Extract the sort key (comment_id) from the lastEvaluatedKey
        // The DynamoDB sort key is mapped to "comment_id" in the Comment domain
        AttributeValue commentIdValue = lastEvaluatedKey.get("comment_id");
        return commentIdValue != null ? commentIdValue.s() : null;
    }
}
