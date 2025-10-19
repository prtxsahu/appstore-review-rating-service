# Cursor-Based Pagination Usage Example

## Overview

The new cursor-based pagination implementation provides better performance for large datasets compared to traditional page-based pagination. This is especially important for DynamoDB where offset-based pagination can be expensive.

## How It Works

Instead of using page numbers, cursor-based pagination uses a "cursor" (the `commentId` of the last item from the previous page) to determine where to start the next page. This allows DynamoDB to efficiently skip to the exact position without scanning through all previous items.

## Usage Examples

### 1. Repository Level (Direct Usage)

```java
// Get first page (no cursor)
PaginatedResult<Comment> firstPage = commentRepository.findTopLevelCommentsByAppId("APP_123", null, 10);

// Get next page using cursor from first page
String nextCursor = firstPage.getNextPageCursor();
if (nextCursor != null) {
    PaginatedResult<Comment> secondPage = commentRepository.findTopLevelCommentsByAppId("APP_123", nextCursor, 10);
}

// Check if there are more pages
if (firstPage.hasNextPage()) {
    // There are more pages available
}
```

### 2. Service Level (Recommended)

```java
// Get first page
PaginatedResult<CommentResponse> firstPage = commentService.getTopLevelCommentsByAppIdWithCursor("APP_123", null, 10);

// Get next page
String nextCursor = firstPage.getNextPageCursor();
if (nextCursor != null) {
    PaginatedResult<CommentResponse> secondPage = commentService.getTopLevelCommentsByAppIdWithCursor("APP_123", nextCursor, 10);
}
```

### 3. Controller Level (API Usage)

The controller now uses cursor-based pagination as the primary method:

#### Cursor-Based Pagination (Primary Endpoint)
```http
GET /apps/{appId}/comments?cursor=COMMENT_123&size=10
```

**Response:**
```json
{
  "items": [
    {
      "commentId": "COMMENT_456",
      "appId": "APP_123",
      "userId": "USER_789",
      "text": "Great app!",
      "createdAt": "2024-01-15T10:30:00Z",
      "likesCount": 5,
      "dislikesCount": 0
    }
  ],
  "lastEvaluatedKey": {
    "appId": {"S": "APP_123"},
    "commentId": {"S": "COMMENT_456"}
  }
}
```

#### Usage Examples:

**First Page:**
```bash
curl "http://localhost:8080/apps/APP_123/comments?size=10"
```

**Next Page:**
```bash
curl "http://localhost:8080/apps/APP_123/comments?cursor=COMMENT_456&size=10"
```

**With Custom Page Size:**
```bash
curl "http://localhost:8080/apps/APP_123/comments?size=20"
```

## Benefits

1. **Performance**: No need to scan through previous pages
2. **Consistency**: Results remain consistent even if new items are added
3. **Scalability**: Works efficiently with large datasets
4. **DynamoDB Optimized**: Uses DynamoDB's native pagination mechanism

## Backward Compatibility

The old page-based methods are still available for backward compatibility:

```java
// Old method (still works, but less efficient for large datasets)
List<CommentResponse> comments = commentService.getTopLevelCommentsByAppId("APP_123", 0, 10);
```

## Migration Guide

1. **For new APIs**: Use the cursor-based pagination methods
2. **For existing APIs**: Gradually migrate to cursor-based pagination
3. **For large datasets**: Prioritize migration to cursor-based pagination

## Response Format

The new pagination returns a `PaginatedResult` object with:

- `items`: List of items for the current page
- `lastEvaluatedKey`: Raw DynamoDB pagination key (for advanced usage)
- `hasNextPage()`: Boolean indicating if more pages are available
- `getNextPageCursor()`: String cursor for the next page (null if no more pages)
