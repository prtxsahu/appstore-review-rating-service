package com.example.appstore.comment.controller;

import com.example.appstore.comment.dto.CommentPageResponse;
import com.example.appstore.comment.dto.CommentRequest;
import com.example.appstore.comment.dto.CommentResponse;
import com.example.appstore.comment.dto.UpdateCommentRequest;
import com.example.appstore.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for comment creation operations.
 * Currently returns dummy responses - will be connected to service layer later.
 */
@Slf4j
@RestController
@RequestMapping("/apps/{appId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * Create a new comment or reply.
     * POST /apps/{appId}/comments
     */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable String appId,
            @Valid @RequestBody CommentRequest request) {
        
        log.info("=== COMMENT CONTROLLER: Creating comment ===");
        log.info("Request received - appId: {}, userId: {}", 
                appId, request.getUserId());
        
        try {
            CommentResponse response = commentService.createComment(appId, request);
            log.info("Comment created successfully - commentId: {}", response.getCommentId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Failed to create comment - appId: {}, error: {}", appId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get paginated comments for an app.
     * GET /apps/{appId}/comments?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<CommentPageResponse> getComments(
            @PathVariable String appId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("=== COMMENT CONTROLLER: Getting comments ===");
        log.info("Request received - appId: {}, page: {}, size: {}", appId, page, size);
        
        try {
            List<CommentResponse> comments = commentService.getTopLevelCommentsByAppId(appId, page, size);
            
            // Determine if there are more comments (hasNext)
            boolean hasNext = comments.size() == size; // If we got exactly the requested size, there might be more
            
            CommentPageResponse response = CommentPageResponse.builder()
                    .comments(comments)
                    .page(page)
                    .size(size)
                    .hasNext(hasNext)
                    .build();
            
            log.info("Comments retrieved successfully - appId: {}, count: {}, page: {}", 
                    appId, comments.size(), page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get comments - appId: {}, error: {}", appId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update a comment by ID.
     * PUT /apps/{appId}/comments/{commentId}
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable String appId,
            @PathVariable String commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        
        log.info("=== COMMENT CONTROLLER: Updating comment ===");
        log.info("Request received - appId: {}, commentId: {}", appId, commentId);
        
        try {
            CommentResponse response = commentService.updateComment(appId, commentId, request);
            log.info("Comment updated successfully - commentId: {}", commentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update comment - appId: {}, commentId: {}, error: {}", 
                    appId, commentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete a comment by ID.
     * DELETE /apps/{appId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String appId,
            @PathVariable String commentId) {
        
        log.info("=== COMMENT CONTROLLER: Deleting comment ===");
        log.info("Request received - appId: {}, commentId: {}", appId, commentId);
        
        try {
            commentService.deleteComment(appId, commentId);
            log.info("Comment deleted successfully - commentId: {}", commentId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete comment - appId: {}, commentId: {}, error: {}", 
                    appId, commentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Like a comment by ID.
     * POST /apps/{appId}/comments/{commentId}/like
     */
    @PostMapping("/{commentId}/like")
    public ResponseEntity<CommentResponse> likeComment(
            @PathVariable String appId,
            @PathVariable String commentId) {
        
        log.info("=== COMMENT CONTROLLER: Liking comment ===");
        log.info("Request received - appId: {}, commentId: {}", appId, commentId);
        
        try {
            CommentResponse response = commentService.likeComment(appId, commentId);
            log.info("Comment liked successfully - commentId: {}, new likes: {}", 
                    commentId, response.getLikesCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to like comment - appId: {}, commentId: {}, error: {}", 
                    appId, commentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Dislike a comment by ID.
     * POST /apps/{appId}/comments/{commentId}/dislike
     */
    @PostMapping("/{commentId}/dislike")
    public ResponseEntity<CommentResponse> dislikeComment(
            @PathVariable String appId,
            @PathVariable String commentId) {
        
        log.info("=== COMMENT CONTROLLER: Disliking comment ===");
        log.info("Request received - appId: {}, commentId: {}", appId, commentId);
        
        try {
            CommentResponse response = commentService.dislikeComment(appId, commentId);
            log.info("Comment disliked successfully - commentId: {}, new dislikes: {}", 
                    commentId, response.getDislikesCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to dislike comment - appId: {}, commentId: {}, error: {}", 
                    appId, commentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create a reply to a comment.
     * POST /apps/{appId}/comments/{commentId}/replies
     */
    @PostMapping("/{commentId}/replies")
    public ResponseEntity<CommentResponse> createReply(
            @PathVariable String appId,
            @PathVariable String commentId,
            @Valid @RequestBody CommentRequest request) {
        
        log.info("=== COMMENT CONTROLLER: Creating reply ===");
        log.info("Request received - appId: {}, parentCommentId: {}, userId: {}", 
                appId, commentId, request.getUserId());
        
        try {
            CommentResponse response = commentService.createReply(appId, commentId, request);
            log.info("Reply created successfully - replyId: {}, parentCommentId: {}", 
                    response.getCommentId(), commentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Failed to create reply - appId: {}, parentCommentId: {}, error: {}", 
                    appId, commentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get replies to a comment.
     * GET /apps/{appId}/comments/{commentId}/replies?page=0&size=10
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<CommentPageResponse> getReplies(
            @PathVariable String appId,
            @PathVariable String commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("=== COMMENT CONTROLLER: Getting replies ===");
        log.info("Request received - appId: {}, parentCommentId: {}, page: {}, size: {}", 
                appId, commentId, page, size);
        
        try {
            List<CommentResponse> replies = commentService.getRepliesByParentId(commentId, page, size);
            
            // Determine if there are more replies (hasNext)
            boolean hasNext = replies.size() == size; // If we got exactly the requested size, there might be more
            
            CommentPageResponse response = CommentPageResponse.builder()
                    .comments(replies)
                    .page(page)
                    .size(size)
                    .hasNext(hasNext)
                    .build();
            
            log.info("Replies retrieved successfully - parentCommentId: {}, count: {}, page: {}", 
                    commentId, replies.size(), page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get replies - appId: {}, parentCommentId: {}, error: {}", 
                    appId, commentId, e.getMessage(), e);
            throw e;
        }
    }
}
