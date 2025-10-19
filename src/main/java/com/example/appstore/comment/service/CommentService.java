package com.example.appstore.comment.service;

import com.example.appstore.comment.domain.Comment;
import com.example.appstore.comment.dto.CommentRequest;
import com.example.appstore.comment.dto.CommentResponse;
import com.example.appstore.shared.dto.SearchResponse;
import com.example.appstore.comment.dto.UpdateCommentRequest;
import com.example.appstore.comment.repository.dynamodb.CommentRepository;
import com.example.appstore.comment.repository.elasticsearch.CommentSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for comment operations with dual-write to DynamoDB and Elasticsearch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService implements CommentServiceInterface {

    private final CommentRepository commentRepository;
    private final CommentSearchRepository commentSearchRepository;

    /**
     * Create a new comment with dual-write to DynamoDB and Elasticsearch.
     */
    public CommentResponse createComment(String appId, CommentRequest request) {
        log.info("=== COMMENT SERVICE: Creating comment ===");
        log.info("Input - appId: {}, userId: {}", 
                appId, request.getUserId());

        try {
            // Generate unique comment ID
            String commentId = "COMMENT_" + UUID.randomUUID().toString().replace("-", "");
            
            // Create comment entity
            Comment comment = Comment.builder()
                    .appId(appId)
                    .commentId(commentId)
                    .userId(request.getUserId())
                    .text(request.getText())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .subCommentCount(0L)
                    .likesCount(0L)
                    .dislikesCount(0L)
                    .build();

            // Dual-write: Save to DynamoDB
            log.info("Saving comment to DynamoDB - commentId: {}", commentId);
            commentRepository.save(comment);

            // Dual-write: Index to Elasticsearch
            log.info("Indexing comment to Elasticsearch - commentId: {}", commentId);
            commentSearchRepository.indexComment(comment);

            // Convert to response DTO
            CommentResponse response = CommentResponse.builder()
                    .commentId(comment.getCommentId())
                    .appId(comment.getAppId())
                    .userId(comment.getUserId())
                    .parentId(comment.getParentId())
                    .text(comment.getText())
                    .createdAt(comment.getCreatedAt())
                    .updatedAt(comment.getUpdatedAt())
                    .subCommentCount(comment.getSubCommentCount())
                    .likesCount(comment.getLikesCount())
                    .dislikesCount(comment.getDislikesCount())
                    .build();

            log.info("Comment created successfully - commentId: {}", commentId);
            log.debug("Response DTO: {}", response);
            return response;

        } catch (Exception e) {
            log.error("Failed to create comment - appId: {}, error: {}", appId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get paginated top-level comments for an app.
     */
    public List<CommentResponse> getTopLevelCommentsByAppId(String appId, int page, int size) {
        log.info("=== COMMENT SERVICE: Getting top-level comments ===");
        log.info("Input - appId: {}, page: {}, size: {}", appId, page, size);

        try {
            log.info("Fetching top-level comments from DynamoDB - appId: {}", appId);
            List<Comment> comments = commentRepository.findTopLevelCommentsByAppId(appId, page, size);
            log.info("Found {} top-level comments for appId: {}", comments.size(), appId);

            // Convert to response DTOs
            List<CommentResponse> responses = comments.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            log.info("Comment retrieval completed successfully - appId: {}, count: {}", appId, responses.size());
            log.debug("Response DTOs: {}", responses);
            return responses;

        } catch (Exception e) {
            log.error("Failed to get comments - appId: {}, error: {}", appId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update a comment by ID with dual-write to DynamoDB and Elasticsearch.
     */
    public CommentResponse updateComment(String appId, String commentId, UpdateCommentRequest request) {
        log.info("=== COMMENT SERVICE: Updating comment ===");
        log.info("Input - appId: {}, commentId: {}", appId, commentId);

        try {
            // Find existing comment
            Comment existingComment = commentRepository.findByAppIdAndCommentId(appId, commentId)
                    .orElseThrow(() -> {
                        log.error("Comment not found - appId: {}, commentId: {}", appId, commentId);
                        return new RuntimeException("Comment not found: " + commentId);
                    });

            log.info("Found existing comment - commentId: {}", commentId);

            // Update comment text and timestamp
            Comment updatedComment = Comment.builder()
                    .appId(existingComment.getAppId())
                    .commentId(existingComment.getCommentId())
                    .userId(existingComment.getUserId())
                    .parentId(existingComment.getParentId())
                    .text(request.getText())
                    .createdAt(existingComment.getCreatedAt())
                    .updatedAt(Instant.now())
                    .subCommentCount(existingComment.getSubCommentCount())
                    .likesCount(existingComment.getLikesCount())
                    .dislikesCount(existingComment.getDislikesCount())
                    .build();

            // Dual-write: Update DynamoDB
            log.info("Updating comment in DynamoDB - commentId: {}", commentId);
            commentRepository.save(updatedComment);

            // Dual-write: Update Elasticsearch
            log.info("Updating comment in Elasticsearch - commentId: {}", commentId);
            commentSearchRepository.updateComment(updatedComment);

            CommentResponse response = convertToResponse(updatedComment);
            log.info("Comment updated successfully - commentId: {}", commentId);
            return response;

        } catch (Exception e) {
            log.error("Failed to update comment - appId: {}, commentId: {}, error: {}", 
                    appId, commentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete a comment by ID with dual-write to DynamoDB and Elasticsearch.
     */
    public void deleteComment(String appId, String commentId) {
        log.info("=== COMMENT SERVICE: Deleting comment ===");
        log.info("Input - appId: {}, commentId: {}", appId, commentId);

        try {
            // Verify comment exists
            commentRepository.findByAppIdAndCommentId(appId, commentId)
                    .orElseThrow(() -> {
                        log.error("Comment not found - appId: {}, commentId: {}", appId, commentId);
                        return new RuntimeException("Comment not found: " + commentId);
                    });

            log.info("Found existing comment - commentId: {}", commentId);

            // Dual-write: Delete from DynamoDB
            log.info("Deleting comment from DynamoDB - commentId: {}", commentId);
            commentRepository.deleteByAppIdAndCommentId(appId, commentId);

            // Dual-write: Delete from Elasticsearch
            log.info("Deleting comment from Elasticsearch - commentId: {}", commentId);
            commentSearchRepository.deleteComment(commentId);

            log.info("Comment deleted successfully - commentId: {}", commentId);

        } catch (Exception e) {
            log.error("Failed to delete comment - appId: {}, commentId: {}, error: {}", 
                    appId, commentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Like a comment by ID.
     */
    public CommentResponse likeComment(String appId, String commentId) {
        log.info("=== COMMENT SERVICE: Liking comment ===");
        log.info("Input - appId: {}, commentId: {}", appId, commentId);

        try {
            // Verify comment exists
            commentRepository.findByAppIdAndCommentId(appId, commentId)
                    .orElseThrow(() -> {
                        log.error("Comment not found - appId: {}, commentId: {}", appId, commentId);
                        return new RuntimeException("Comment not found: " + commentId);
                    });

            log.info("Found existing comment - commentId: {}", commentId);

            // Increment likes count
            log.info("Incrementing likes count for comment - commentId: {}", commentId);
            commentRepository.incrementLikesCount(appId, commentId);

            // Get updated comment
            Comment updatedComment = commentRepository.findByAppIdAndCommentId(appId, commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found after update"));

            // Update Elasticsearch with new likes count
            log.info("Updating Elasticsearch with new likes count - commentId: {}", commentId);
            commentSearchRepository.updateComment(updatedComment);

            CommentResponse response = convertToResponse(updatedComment);
            log.info("Comment liked successfully - commentId: {}, new likes: {}", 
                    commentId, response.getLikesCount());
            return response;

        } catch (Exception e) {
            log.error("Failed to like comment - appId: {}, commentId: {}, error: {}", 
                    appId, commentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Dislike a comment by ID.
     */
    public CommentResponse dislikeComment(String appId, String commentId) {
        log.info("=== COMMENT SERVICE: Disliking comment ===");
        log.info("Input - appId: {}, commentId: {}", appId, commentId);

        try {
            // Verify comment exists
            commentRepository.findByAppIdAndCommentId(appId, commentId)
                    .orElseThrow(() -> {
                        log.error("Comment not found - appId: {}, commentId: {}", appId, commentId);
                        return new RuntimeException("Comment not found: " + commentId);
                    });

            log.info("Found existing comment - commentId: {}", commentId);

            // Increment dislikes count
            log.info("Incrementing dislikes count for comment - commentId: {}", commentId);
            commentRepository.incrementDislikesCount(appId, commentId);

            // Get updated comment
            Comment updatedComment = commentRepository.findByAppIdAndCommentId(appId, commentId)
                    .orElseThrow(() -> new RuntimeException("Comment not found after update"));

            // Update Elasticsearch with new dislikes count
            log.info("Updating Elasticsearch with new dislikes count - commentId: {}", commentId);
            commentSearchRepository.updateComment(updatedComment);

            CommentResponse response = convertToResponse(updatedComment);
            log.info("Comment disliked successfully - commentId: {}, new dislikes: {}", 
                    commentId, response.getDislikesCount());
            return response;

        } catch (Exception e) {
            log.error("Failed to dislike comment - appId: {}, commentId: {}, error: {}", 
                    appId, commentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create a reply to a comment (sub-comment).
     * Only saves to DynamoDB, not indexed in Elasticsearch.
     */
    public CommentResponse createReply(String appId, String parentCommentId, CommentRequest request) {
        log.info("=== COMMENT SERVICE: Creating reply ===");
        log.info("Input - appId: {}, parentCommentId: {}, userId: {}", 
                appId, parentCommentId, request.getUserId());

        try {
            // 1. Validate that parent comment exists and is not null
            log.info("Validating parent comment exists - parentCommentId: {}", parentCommentId);
            commentRepository.findByAppIdAndCommentId(appId, parentCommentId)
                    .orElseThrow(() -> {
                        log.error("Parent comment not found - appId: {}, parentCommentId: {}", appId, parentCommentId);
                        return new RuntimeException("Parent comment not found: " + parentCommentId);
                    });

            log.info("Parent comment validated - parentCommentId: {}", parentCommentId);

            // 2. Generate unique reply ID
            String replyId = "COMMENT_" + UUID.randomUUID().toString().replace("-", "");
            
            // 3. Create reply entity (sub-comment)
            Comment reply = Comment.builder()
                    .appId(appId)
                    .commentId(replyId)
                    .userId(request.getUserId())
                    .parentId(parentCommentId) // Set parent ID
                    .text(request.getText())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .subCommentCount(0L) // Replies don't have sub-comments
                    .likesCount(0L)
                    .dislikesCount(0L)
                    .build();

            // 4. Save reply to DynamoDB only (no Elasticsearch indexing for replies)
            log.info("Saving reply to DynamoDB - replyId: {}", replyId);
            commentRepository.save(reply);

            // 5. Increment parent comment's subCommentCount
            log.info("Incrementing parent comment's subCommentCount - parentCommentId: {}", parentCommentId);
            commentRepository.incrementSubCommentCount(appId, parentCommentId);

            // 6. Convert to response DTO
            CommentResponse response = convertToResponse(reply);

            log.info("Reply created successfully - replyId: {}, parentCommentId: {}", replyId, parentCommentId);
            log.debug("Response DTO: {}", response);
            return response;

        } catch (Exception e) {
            log.error("Failed to create reply - appId: {}, parentCommentId: {}, error: {}", 
                    appId, parentCommentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get paginated replies for a comment.
     */
    public List<CommentResponse> getRepliesByParentId(String parentCommentId, int page, int size) {
        log.info("=== COMMENT SERVICE: Getting replies ===");
        log.info("Input - parentCommentId: {}, page: {}, size: {}", parentCommentId, page, size);

        try {
            log.info("Fetching replies from DynamoDB - parentCommentId: {}", parentCommentId);
            List<Comment> replies = commentRepository.findRepliesByParentId(parentCommentId, page, size);
            log.info("Found {} replies for parentCommentId: {}", replies.size(), parentCommentId);

            // Convert to response DTOs
            List<CommentResponse> responses = replies.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            log.info("Replies retrieval completed successfully - parentCommentId: {}, count: {}", 
                    parentCommentId, responses.size());
            log.debug("Response DTOs: {}", responses);
            return responses;

        } catch (Exception e) {
            log.error("Failed to get replies - parentCommentId: {}, error: {}", 
                    parentCommentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Convert Comment entity to CommentResponse DTO.
     */
    private CommentResponse convertToResponse(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .appId(comment.getAppId())
                .userId(comment.getUserId())
                .parentId(comment.getParentId())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .subCommentCount(comment.getSubCommentCount())
                .likesCount(comment.getLikesCount())
                .dislikesCount(comment.getDislikesCount())
                .build();
    }

    /**
     * Search comments within a specific app by keyword with pagination.
     */
    public SearchResponse<CommentResponse> searchCommentsByAppId(String appId, String keyword, int page, int size) {
        log.info("=== COMMENT SERVICE: Searching comments ===");
        log.info("Input - appId: {}, keyword: {}, page: {}, size: {}", appId, keyword, page, size);
        
        try {
            // Search comments in Elasticsearch using repository method
            SearchResponse<Comment> searchResponse = commentSearchRepository.searchCommentsByAppId(appId, keyword, page, size);
            
            // Get comments from repository response
            List<Comment> comments = searchResponse.getItems();
            
            // Convert Comment domain objects to CommentResponse DTOs
            List<CommentResponse> commentResponses = comments.stream()
                    .map(this::convertToResponse)
                    .toList();
            
            // Build response with pagination info from repository
            SearchResponse<CommentResponse> response = SearchResponse.<CommentResponse>builder()
                    .items(commentResponses)
                    .page(page)
                    .size(size)
                    .totalElements(searchResponse.getTotalElements())
                    .totalPages(searchResponse.getTotalPages())
                    .hasNext(searchResponse.getHasNext())
                    .hasPrevious(searchResponse.getHasPrevious())
                    .build();
            
            log.info("Comment search completed successfully - appId: {}, keyword: {}, results: {}", 
                    appId, keyword, commentResponses.size());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error searching comments - appId: {}, keyword: {}, error: {}", appId, keyword, e.getMessage(), e);
            throw new RuntimeException("Failed to search comments", e);
        }
    }

    /**
     * Get all comments made by a specific user for an app.
     */
    public List<CommentResponse> getCommentsByUserForApp(String appId, String userId) {
        log.info("=== COMMENT SERVICE: Getting user comments ===");
        log.info("Input - appId: {}, userId: {}", appId, userId);

        try {
            log.info("Fetching user comments from DynamoDB - appId: {}, userId: {}", appId, userId);
            List<Comment> userComments = commentRepository.findByAppIdAndUserId(appId, userId);
            log.info("Found {} user comments for appId: {}, userId: {}", userComments.size(), appId, userId);

            // Convert to response DTOs
            List<CommentResponse> responses = userComments.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            log.info("User comments retrieval completed successfully - appId: {}, userId: {}, count: {}", 
                    appId, userId, responses.size());
            log.debug("Response DTOs: {}", responses);
            return responses;

        } catch (Exception e) {
            log.error("Failed to get user comments - appId: {}, userId: {}, error: {}", appId, userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get paginated top-level comments for an app excluding comments by a specific user.
     */
    public List<CommentResponse> getTopLevelCommentsByAppIdExcludingUser(String appId, String userId, int page, int size) {
        log.info("=== COMMENT SERVICE: Getting top-level comments excluding user ===");
        log.info("Input - appId: {}, userId: {}, page: {}, size: {}", appId, userId, page, size);

        try {
            log.info("Fetching top-level comments from DynamoDB excluding user - appId: {}, userId: {}", appId, userId);
            List<Comment> comments = commentRepository.findTopLevelCommentsByAppIdExcludingUser(appId, userId, page, size);
            log.info("Found {} top-level comments for appId: {} excluding user: {}", comments.size(), appId, userId);

            // Convert to response DTOs
            List<CommentResponse> responses = comments.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            log.info("Comment retrieval completed successfully - appId: {}, userId: {}, count: {}", appId, userId, responses.size());
            log.debug("Response DTOs: {}", responses);
            return responses;

        } catch (Exception e) {
            log.error("Failed to get comments excluding user - appId: {}, userId: {}, error: {}", appId, userId, e.getMessage(), e);
            throw e;
        }
    }
}
