package com.example.appstore.comment.service;

import com.example.appstore.comment.dto.CommentRequest;
import com.example.appstore.comment.dto.CommentResponse;
import com.example.appstore.comment.dto.UpdateCommentRequest;
import com.example.appstore.shared.dto.PaginatedResult;
import com.example.appstore.shared.dto.SearchResponse;

import java.util.List;

/**
 * Interface for Comment service operations.
 * Defines the contract for comment management business logic.
 */
public interface CommentServiceInterface {
    
    /**
     * Create a new comment for an app.
     * 
     * @param appId The app ID
     * @param request The comment creation request
     * @return The created comment response
     */
    CommentResponse createComment(String appId, CommentRequest request);
    
    /**
     * Get top-level comments for an app with pagination.
     * 
     * @param appId The app ID
     * @param page The page number (0-based)
     * @param size The page size
     * @return List of top-level comment responses
     */
    List<CommentResponse> getTopLevelCommentsByAppId(String appId, int page, int size);
    
    /**
     * Update an existing comment.
     * 
     * @param appId The app ID
     * @param commentId The comment ID
     * @param request The update request
     * @return The updated comment response
     */
    CommentResponse updateComment(String appId, String commentId, UpdateCommentRequest request);
    
    /**
     * Delete a comment.
     * 
     * @param appId The app ID
     * @param commentId The comment ID
     */
    void deleteComment(String appId, String commentId);
    
    /**
     * Like a comment.
     * 
     * @param appId The app ID
     * @param commentId The comment ID
     * @return The updated comment response
     */
    CommentResponse likeComment(String appId, String commentId);
    
    /**
     * Dislike a comment.
     * 
     * @param appId The app ID
     * @param commentId The comment ID
     * @return The updated comment response
     */
    CommentResponse dislikeComment(String appId, String commentId);
    
    /**
     * Create a reply to a comment.
     * 
     * @param appId The app ID
     * @param parentCommentId The parent comment ID
     * @param request The reply creation request
     * @return The created reply response
     */
    CommentResponse createReply(String appId, String parentCommentId, CommentRequest request);
    
    /**
     * Get replies for a parent comment with pagination.
     * 
     * @param parentCommentId The parent comment ID
     * @param page The page number (0-based)
     * @param size The page size
     * @return List of reply responses
     */
    List<CommentResponse> getRepliesByParentId(String parentCommentId, int page, int size);
    
    /**
     * Search comments for an app by keyword.
     * 
     * @param appId The app ID
     * @param keyword The search keyword
     * @param page The page number (0-based)
     * @param size The page size
     * @return Search response with matching comments
     */
    SearchResponse<CommentResponse> searchCommentsByAppId(String appId, String keyword, int page, int size);
    
    /**
     * Get comments by a specific user for an app.
     * 
     * @param appId The app ID
     * @param userId The user ID
     * @return List of comment responses by the user
     */
    List<CommentResponse> getCommentsByUserForApp(String appId, String userId);


    PaginatedResult<CommentResponse> getTopLevelCommentsByAppIdWithCursor(String appId, String cursor, int pageSize);

    
    /**
     * Get top-level comments for an app excluding a specific user.
     * 
     * @param appId The app ID
     * @param userId The user ID to exclude
     * @param page The page number (0-based)
     * @param size The page size
     * @return List of top-level comment responses excluding the user
     */
    List<CommentResponse> getTopLevelCommentsByAppIdExcludingUser(String appId, String userId, int page, int size);
}
