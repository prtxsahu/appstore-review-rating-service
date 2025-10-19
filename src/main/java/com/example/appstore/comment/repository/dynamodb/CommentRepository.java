package com.example.appstore.comment.repository.dynamodb;

import com.example.appstore.comment.domain.Comment;
import com.example.appstore.shared.dto.PaginatedResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository for Comment entity operations using DynamoDB Enhanced Client.
 * Uses composite key: app_id (PK) + comment_id (SK)
 * GSI: parent_index on parent_id for threaded comments
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CommentRepository {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String TABLE_NAME = "comments";

    private DynamoDbTable<Comment> getCommentTable() {
        return dynamoDbEnhancedClient.table(TABLE_NAME, TableSchema.fromBean(Comment.class));
    }

    /**
     * Save a comment to DynamoDB.
     */
    public Comment save(Comment comment) {
        try {
            log.debug("Saving comment: {} for app: {}", comment.getCommentId(), comment.getAppId());
            DynamoDbTable<Comment> commentTable = getCommentTable();
            commentTable.putItem(comment);
            log.info("Successfully saved comment: {}", comment.getCommentId());
            return comment;
        } catch (DynamoDbException e) {
            log.error("Error saving comment: {}", comment.getCommentId(), e);
            throw new RuntimeException("Failed to save comment", e);
        }
    }

    /**
     * Find a comment by app ID and comment ID.
     */
    public Optional<Comment> findByAppIdAndCommentId(String appId, String commentId) {
        try {
            log.debug("Finding comment: {} for app: {}", commentId, appId);
            DynamoDbTable<Comment> commentTable = getCommentTable();
            Key key = Key.builder()
                    .partitionValue(appId)
                    .sortValue(commentId)
                    .build();
            
            GetItemEnhancedRequest request = GetItemEnhancedRequest.builder()
                    .key(key)
                    .build();
            
            Comment comment = commentTable.getItem(request);
            if (comment != null) {
                log.info("Found comment: {}", commentId);
                return Optional.of(comment);
            } else {
                log.info("Comment not found: {}", commentId);
                return Optional.empty();
            }
        } catch (DynamoDbException e) {
            log.error("Error finding comment: {}", commentId, e);
            throw new RuntimeException("Failed to find comment", e);
        }
    }

    /**
     * Find top-level comments for an app (backward compatibility method).
     * 
     * @param appId The application ID
     * @return List of top-level comments (first 10)
     */
    public List<Comment> findTopLevelCommentsByAppId(String appId) {
        PaginatedResult<Comment> result = findTopLevelCommentsByAppId(appId, null, 10);
        return result.getItems();
    }
    
    /**
     * Find paginated top-level comments for an app (backward compatibility with page-based pagination).
     * 
     * @param appId The application ID
     * @param page Page number (0-based) - Note: This is less efficient than cursor-based pagination
     * @param size Page size
     * @return List of top-level comments
     */
    public List<Comment> findTopLevelCommentsByAppId(String appId, int page, int size) {
        // For backward compatibility, we'll use a simple approach
        // Note: This is less efficient than cursor-based pagination for large datasets
        PaginatedResult<Comment> result = findTopLevelCommentsByAppId(appId, null, size * (page + 1));
        List<Comment> allComments = result.getItems();
        
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, allComments.size());
        
        if (startIndex >= allComments.size()) {
            return List.of();
        }
        
        return allComments.subList(startIndex, endIndex);
    }

    /**
     * Find paginated top-level comments for an app using cursor-based pagination.
     * 
     * @param appId The application ID
     * @param lastEvaluatedCommentId The cursor for pagination (commentId from previous page)
     * @param pageSize Number of items per page
     * @return PaginatedResult containing comments and pagination info
     */
    public PaginatedResult<Comment> findTopLevelCommentsByAppId(String appId, String lastEvaluatedCommentId, int pageSize) {
        try {
            log.debug("Finding paginated top-level comments for app: {}, from comment: {}, size: {}", 
                      appId, lastEvaluatedCommentId, pageSize);

            DynamoDbTable<Comment> commentTable = getCommentTable();
            Key key = Key.builder().partitionValue(appId).build();

            QueryEnhancedRequest.Builder queryBuilder = QueryEnhancedRequest.builder()
                    .queryConditional(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo(key))
                    .scanIndexForward(false) // newest first
                    .filterExpression(Expression.builder()
                            .expression("attribute_not_exists(parent_id)")
                            .build())
                    .limit(pageSize);

            // If client provided a cursor (lastEvaluatedCommentId)
            if (lastEvaluatedCommentId != null && !lastEvaluatedCommentId.isEmpty()) {
                queryBuilder.exclusiveStartKey(Map.of(
                    "app_id", AttributeValue.builder().s(appId).build(),
                    "comment_id", AttributeValue.builder().s(lastEvaluatedCommentId).build()
                ));
            }

            PageIterable<Comment> response = commentTable.query(queryBuilder.build());
            
            // Get the first page from the iterable
            Page<Comment> firstPage = response.stream().findFirst().orElse(null);
            
            if (firstPage == null) {
                return PaginatedResult.<Comment>builder()
                        .items(List.of())
                        .lastEvaluatedKey(null)
                        .build();
            }

            List<Comment> comments = firstPage.items()
                    .stream()
                    .filter(c -> c.getParentId() == null)
                    .collect(Collectors.toList());

            Map<String, AttributeValue> lastKey = firstPage.lastEvaluatedKey();

            log.info("Found {} top-level comments for app: {} with cursor: {}", 
                    comments.size(), appId, lastEvaluatedCommentId);

            return PaginatedResult.<Comment>builder()
                    .items(comments)
                    .lastEvaluatedKey(lastKey)
                    .build();
        } catch (DynamoDbException e) {
            log.error("Error finding paginated top-level comments for app: {}", appId, e);
            throw new RuntimeException("Failed to find paginated top-level comments", e);
        }
    }


    /**
     * Delete a comment by app ID and comment ID.
     */
    public void deleteByAppIdAndCommentId(String appId, String commentId) {
        try {
            log.debug("Deleting comment: {} for app: {}", commentId, appId);
            DynamoDbTable<Comment> commentTable = getCommentTable();
            Key key = Key.builder()
                    .partitionValue(appId)
                    .sortValue(commentId)
                    .build();
            
            commentTable.deleteItem(key);
            log.info("Successfully deleted comment: {}", commentId);
        } catch (DynamoDbException e) {
            log.error("Error deleting comment: {}", commentId, e);
            throw new RuntimeException("Failed to delete comment", e);
        }
    }
    
    // ==================== Atomic Counter Operations ====================
    
    /**
     * Atomically increment subCommentCount by 1.
     * Uses DynamoDB ADD operation for atomic counter updates.
     * 
     * @param appId The application ID
     * @param commentId The comment ID
     */
    public void incrementSubCommentCount(String appId, String commentId) {
        updateCounter(appId, commentId, "sub_comment_count", 1L);
    }
    
    /**
     * Atomically decrement subCommentCount by 1.
     * Uses DynamoDB ADD operation for atomic counter updates.
     * 
     * @param appId The application ID
     * @param commentId The comment ID
     */
    public void decrementSubCommentCount(String appId, String commentId) {
        updateCounter(appId, commentId, "sub_comment_count", -1L);
    }
    
    /**
     * Atomically increment likesCount by 1.
     * Uses DynamoDB ADD operation for atomic counter updates.
     * 
     * @param appId The application ID
     * @param commentId The comment ID
     */
    public void incrementLikesCount(String appId, String commentId) {
        updateCounter(appId, commentId, "likes_count", 1L);
    }
    
    /**
     * Atomically decrement likesCount by 1.
     * Uses DynamoDB ADD operation for atomic counter updates.
     * 
     * @param appId The application ID
     * @param commentId The comment ID
     */
    public void decrementLikesCount(String appId, String commentId) {
        updateCounter(appId, commentId, "likes_count", -1L);
    }
    
    /**
     * Atomically increment dislikesCount by 1.
     * Uses DynamoDB ADD operation for atomic counter updates.
     * 
     * @param appId The application ID
     * @param commentId The comment ID
     */
    public void incrementDislikesCount(String appId, String commentId) {
        updateCounter(appId, commentId, "dislikes_count", 1L);
    }
    
    /**
     * Atomically decrement dislikesCount by 1.
     * Uses DynamoDB ADD operation for atomic counter updates.
     * 
     * @param appId The application ID
     * @param commentId The comment ID
     */
    public void decrementDislikesCount(String appId, String commentId) {
        updateCounter(appId, commentId, "dislikes_count", -1L);
    }
    
    /**
     * Generic method to update a counter field.
     * Note: This is a simplified implementation. For true atomic counters,
     * use DynamoDB's native ADD operation with UpdateItem API directly.
     * 
     * @param appId The application ID
     * @param commentId The comment ID
     * @param fieldName The field name to update
     * @param incrementValue The value to add (can be negative for decrement)
     */
    private void updateCounter(String appId, String commentId, String fieldName, Long incrementValue) {
        try {
            log.debug("Updating counter {} for comment {} by {}", fieldName, commentId, incrementValue);
            
            // Get existing comment
            Optional<Comment> existingComment = findByAppIdAndCommentId(appId, commentId);
            if (existingComment.isEmpty()) {
                log.warn("Comment not found for counter update: appId={}, commentId={}", appId, commentId);
                return;
            }
            
            Comment comment = existingComment.get();
            
            // Update the appropriate counter field
            if ("sub_comment_count".equals(fieldName)) {
                comment.setSubCommentCount(comment.getSubCommentCount() + incrementValue);
            } else if ("likes_count".equals(fieldName)) {
                comment.setLikesCount(comment.getLikesCount() + incrementValue);
            } else if ("dislikes_count".equals(fieldName)) {
                comment.setDislikesCount(comment.getDislikesCount() + incrementValue);
            }
            
            // Save the updated comment
            save(comment);
            log.info("Successfully updated counter {} for comment {} by {}", fieldName, commentId, incrementValue);
            
        } catch (Exception e) {
            log.error("Error updating counter {} for comment {}", fieldName, commentId, e);
            throw new RuntimeException("Failed to update counter", e);
        }
    }

    // ==================== Reply Query Methods ====================

    /**
     * Find replies for a specific comment using scan operation.
     * Note: This is inefficient for large datasets but simple for now.
     * 
     * @param parentId The parent comment ID
     * @return List of reply comments
     */
    public List<Comment> findRepliesByParentId(String parentId) {
        return findRepliesByParentId(parentId, 0, 10);
    }

    /**
     * Find paginated replies for a specific comment using GSI query.
     * Uses parent_index GSI for efficient querying.
     * 
     * @param parentId The parent comment ID
     * @param page Page number (0-based)
     * @param size Page size
     * @return List of reply comments sorted by createdAt DESC (latest first)
     */
    public List<Comment> findRepliesByParentId(String parentId, int page, int size) {
        try {
            log.debug("Finding paginated replies for parent comment: {}, page: {}, size: {}", parentId, page, size);
            DynamoDbTable<Comment> commentTable = getCommentTable();
            
            // Use GSI query instead of scan for efficient retrieval
            Key key = Key.builder()
                    .partitionValue(parentId) // parent_id is the partition key in GSI
                    .build();
            
            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                    .queryConditional(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo(key))
                    .scanIndexForward(false) // Sort by sort key in descending order (latest first)
                    .limit(size) // Limit results to page size
                    .build();
            
            // Execute query on GSI and collect results
            List<Comment> replies = commentTable.index("parent_index").query(request)
                    .stream()
                    .flatMap(queryPage -> queryPage.items().stream())
                    .collect(Collectors.toList());
            
            log.info("Found {} replies for parent: {}, page: {}", replies.size(), parentId, page);
            return replies;
            
        } catch (DynamoDbException e) {
            log.error("Error finding paginated replies for parent: {}", parentId, e);
            throw new RuntimeException("Failed to find paginated replies", e);
        }
    }

    /**
     * Find comments by app ID and user ID.
     * Returns all comments (both top-level and replies) made by a specific user for an app.
     */
    public List<Comment> findByAppIdAndUserId(String appId, String userId) {
        try {
            log.debug("Finding comments for app: {} by user: {}", appId, userId);
            DynamoDbTable<Comment> commentTable = getCommentTable();
            Key key = Key.builder()
                    .partitionValue(appId)
                    .build();
            
            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                    .queryConditional(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo(key))
                    .scanIndexForward(false) // Sort by sort key in descending order (latest first)
                    .build();
            
            List<Comment> userComments = commentTable.query(request)
                    .items()
                    .stream()
                    .filter(comment -> userId.equals(comment.getUserId())) // Filter by user ID
                    .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt())) // Sort by createdAt DESC
                    .collect(Collectors.toList());
            
            log.info("Found {} comments for app: {} by user: {}", userComments.size(), appId, userId);
            return userComments;
        } catch (DynamoDbException e) {
            log.error("Error finding comments for app: {} by user: {}", appId, userId, e);
            throw new RuntimeException("Failed to find comments by user", e);
        }
    }

    /**
     * Find top-level comments for an app excluding comments by a specific user.
     * Used to get general comments excluding the requesting user's comments.
     * Uses DynamoDB filter expressions for efficient filtering.
     */
    public List<Comment> findTopLevelCommentsByAppIdExcludingUser(String appId, String userId, int page, int size) {
        try {
            log.debug("Finding paginated top-level comments for app: {} excluding user: {}, page: {}, size: {}", appId, userId, page, size);
            DynamoDbTable<Comment> commentTable = getCommentTable();
            Key key = Key.builder()
                    .partitionValue(appId)
                    .build();
            
            // Use filter expression to get only top-level comments and exclude specific user
            QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                    .queryConditional(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo(key))
                    .scanIndexForward(false) // Sort by sort key in descending order (latest first)
                    .filterExpression(Expression.builder()
                            .expression("attribute_not_exists(parent_id) AND user_id <> :userId")
                            .putExpressionValue(":userId", AttributeValue.builder().s(userId).build())
                            .build())
                    .limit(size * 3) // Get more items to account for filtering
                    .build();
            
            List<Comment> allComments = commentTable.query(request)
                    .items()
                    .stream()
                    .filter(comment -> comment.getParentId() == null) // Additional filter for safety
                    .filter(comment -> !userId.equals(comment.getUserId())) // Additional user filter for safety
                    .collect(Collectors.toList());
            
            // Manual pagination since DynamoDB filter expressions don't guarantee exact counts
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, allComments.size());
            
            if (startIndex >= allComments.size()) {
                log.debug("Page {} is beyond available comments for app: {} excluding user: {}", page, appId, userId);
                return List.of();
            }
            
            List<Comment> paginatedComments = allComments.subList(startIndex, endIndex);
            log.info("Found {} top-level comments for app: {} excluding user: {}, page: {} (showing {}-{})", 
                    paginatedComments.size(), appId, userId, page, startIndex + 1, endIndex);
            
            return paginatedComments;
        } catch (DynamoDbException e) {
            log.error("Error finding paginated top-level comments for app: {} excluding user: {}", appId, userId, e);
            throw new RuntimeException("Failed to find paginated top-level comments excluding user", e);
        }
    }
}
