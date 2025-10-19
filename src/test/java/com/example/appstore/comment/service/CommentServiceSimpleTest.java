package com.example.appstore.comment.service;

import com.example.appstore.comment.domain.Comment;
import com.example.appstore.comment.dto.CommentRequest;
import com.example.appstore.comment.dto.CommentResponse;
import com.example.appstore.comment.dto.UpdateCommentRequest;
import com.example.appstore.comment.repository.dynamodb.CommentRepository;
import com.example.appstore.comment.repository.elasticsearch.CommentSearchRepository;
import com.example.appstore.shared.dto.SearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Simple unit tests for CommentService.
 * Tests the business logic for comment management operations with dual-write pattern.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Simple Tests")
class CommentServiceSimpleTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentSearchRepository commentSearchRepository;

    @InjectMocks
    private CommentService commentService;

    private CommentRequest validCommentRequest;
    private Comment validComment;
    private UpdateCommentRequest validUpdateRequest;

    @BeforeEach
    void setUp() {
        // Setup test data
        validCommentRequest = CommentRequest.builder()
                .userId("USER_123")
                .text("This is a test comment")
                .build();

        validComment = Comment.builder()
                .commentId("COMMENT_123")
                .appId("APP_123")
                .userId("USER_123")
                .text("This is a test comment")
                .parentId(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .subCommentCount(0L)
                .likesCount(0L)
                .dislikesCount(0L)
                .build();


        validUpdateRequest = UpdateCommentRequest.builder()
                .text("Updated comment text")
                .build();
    }

    @Test
    @DisplayName("Should create comment successfully with dual-write")
    void createComment_WithValidRequest_ShouldReturnCommentResponse() {
        // Given
        String appId = "APP_123";
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setCommentId("COMMENT_123");
            return comment;
        });
        doNothing().when(commentSearchRepository).indexComment(any(Comment.class));

        // When
        CommentResponse result = commentService.createComment(appId, validCommentRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCommentId()).isEqualTo("COMMENT_123");
        assertThat(result.getAppId()).isEqualTo("APP_123");
        assertThat(result.getUserId()).isEqualTo("USER_123");
        assertThat(result.getText()).isEqualTo("This is a test comment");

        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(commentSearchRepository, times(1)).indexComment(any(Comment.class));
    }

    @Test
    @DisplayName("Should create comment with generated UUID")
    void createComment_ShouldGenerateUniqueCommentId() {
        // Given
        String appId = "APP_123";
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setCommentId("COMMENT_generated_123");
            return comment;
        });
        doNothing().when(commentSearchRepository).indexComment(any(Comment.class));

        // When
        CommentResponse result = commentService.createComment(appId, validCommentRequest);

        // Then
        assertThat(result.getCommentId()).isNotNull();
        assertThat(result.getCommentId()).startsWith("COMMENT_");
        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(commentSearchRepository, times(1)).indexComment(any(Comment.class));
    }

    @Test
    @DisplayName("Should get top-level comments by app ID with pagination")
    void getTopLevelCommentsByAppId_WithValidAppId_ShouldReturnComments() {
        // Given
        String appId = "APP_123";
        int page = 0;
        int size = 10;
        List<Comment> comments = Arrays.asList(validComment);
        when(commentRepository.findTopLevelCommentsByAppId(appId, page, size)).thenReturn(comments);

        // When
        List<CommentResponse> result = commentService.getTopLevelCommentsByAppId(appId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCommentId()).isEqualTo("COMMENT_123");
        assertThat(result.get(0).getAppId()).isEqualTo("APP_123");

        verify(commentRepository, times(1)).findTopLevelCommentsByAppId(appId, page, size);
    }

    @Test
    @DisplayName("Should update comment successfully")
    void updateComment_WithValidRequest_ShouldReturnUpdatedComment() {
        // Given
        String appId = "APP_123";
        String commentId = "COMMENT_123";
        Comment updatedComment = Comment.builder()
                .commentId("COMMENT_123")
                .appId("APP_123")
                .userId("USER_123")
                .text("Updated comment text")
                .parentId(null)
                .createdAt(validComment.getCreatedAt())
                .updatedAt(Instant.now())
                .subCommentCount(0L)
                .likesCount(0L)
                .dislikesCount(0L)
                .build();

        when(commentRepository.findByAppIdAndCommentId(appId, commentId)).thenReturn(Optional.of(validComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(updatedComment);
        doNothing().when(commentSearchRepository).updateComment(any(Comment.class));

        // When
        CommentResponse result = commentService.updateComment(appId, commentId, validUpdateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCommentId()).isEqualTo("COMMENT_123");
        assertThat(result.getText()).isEqualTo("Updated comment text");

        verify(commentRepository, times(1)).findByAppIdAndCommentId(appId, commentId);
        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(commentSearchRepository, times(1)).updateComment(any(Comment.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent comment")
    void updateComment_WithNonExistentComment_ShouldThrowException() {
        // Given
        String appId = "APP_123";
        String commentId = "COMMENT_NON_EXISTENT";
        when(commentRepository.findByAppIdAndCommentId(appId, commentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.updateComment(appId, commentId, validUpdateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Comment not found");

        verify(commentRepository, times(1)).findByAppIdAndCommentId(appId, commentId);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should delete comment successfully")
    void deleteComment_WithValidComment_ShouldDeleteComment() {
        // Given
        String appId = "APP_123";
        String commentId = "COMMENT_123";
        when(commentRepository.findByAppIdAndCommentId(appId, commentId)).thenReturn(Optional.of(validComment));
        doNothing().when(commentRepository).deleteByAppIdAndCommentId(appId, commentId);
        doNothing().when(commentSearchRepository).deleteComment(commentId);

        // When
        commentService.deleteComment(appId, commentId);

        // Then
        verify(commentRepository, times(1)).findByAppIdAndCommentId(appId, commentId);
        verify(commentRepository, times(1)).deleteByAppIdAndCommentId(appId, commentId);
        verify(commentSearchRepository, times(1)).deleteComment(commentId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent comment")
    void deleteComment_WithNonExistentComment_ShouldThrowException() {
        // Given
        String appId = "APP_123";
        String commentId = "COMMENT_NON_EXISTENT";
        when(commentRepository.findByAppIdAndCommentId(appId, commentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(appId, commentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Comment not found");

        verify(commentRepository, times(1)).findByAppIdAndCommentId(appId, commentId);
        verify(commentRepository, never()).deleteByAppIdAndCommentId(anyString(), anyString());
    }

    @Test
    @DisplayName("Should like comment successfully")
    void likeComment_WithValidComment_ShouldReturnUpdatedComment() {
        // Given
        String appId = "APP_123";
        String commentId = "COMMENT_123";
        when(commentRepository.findByAppIdAndCommentId(appId, commentId)).thenReturn(Optional.of(validComment));
        doNothing().when(commentRepository).incrementLikesCount(appId, commentId);

        // When
        CommentResponse result = commentService.likeComment(appId, commentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCommentId()).isEqualTo("COMMENT_123");

        verify(commentRepository, times(2)).findByAppIdAndCommentId(appId, commentId);
        verify(commentRepository, times(1)).incrementLikesCount(appId, commentId);
    }

    @Test
    @DisplayName("Should dislike comment successfully")
    void dislikeComment_WithValidComment_ShouldReturnUpdatedComment() {
        // Given
        String appId = "APP_123";
        String commentId = "COMMENT_123";
        when(commentRepository.findByAppIdAndCommentId(appId, commentId)).thenReturn(Optional.of(validComment));
        doNothing().when(commentRepository).incrementDislikesCount(appId, commentId);

        // When
        CommentResponse result = commentService.dislikeComment(appId, commentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCommentId()).isEqualTo("COMMENT_123");

        verify(commentRepository, times(2)).findByAppIdAndCommentId(appId, commentId);
        verify(commentRepository, times(1)).incrementDislikesCount(appId, commentId);
    }

    @Test
    @DisplayName("Should create reply successfully")
    void createReply_WithValidRequest_ShouldReturnReplyResponse() {
        // Given
        String appId = "APP_123";
        String parentCommentId = "COMMENT_PARENT_123";
        Comment parentComment = Comment.builder()
                .commentId(parentCommentId)
                .appId(appId)
                .userId("USER_PARENT")
                .text("Parent comment")
                .parentId(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .subCommentCount(0L)
                .likesCount(0L)
                .dislikesCount(0L)
                .build();

        Comment reply = Comment.builder()
                .commentId("COMMENT_REPLY_123")
                .appId(appId)
                .userId("USER_123")
                .text("This is a reply")
                .parentId(parentCommentId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .subCommentCount(0L)
                .likesCount(0L)
                .dislikesCount(0L)
                .build();

        when(commentRepository.findByAppIdAndCommentId(appId, parentCommentId)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setCommentId("COMMENT_REPLY_123");
            return comment;
        });
        doNothing().when(commentRepository).incrementSubCommentCount(appId, parentCommentId);

        // When
        CommentResponse result = commentService.createReply(appId, parentCommentId, validCommentRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCommentId()).isEqualTo("COMMENT_REPLY_123");
        assertThat(result.getParentId()).isEqualTo(parentCommentId);

        verify(commentRepository, times(1)).findByAppIdAndCommentId(appId, parentCommentId);
        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(commentSearchRepository, never()).indexComment(any(Comment.class));
        verify(commentRepository, times(1)).incrementSubCommentCount(appId, parentCommentId);
    }

    @Test
    @DisplayName("Should throw exception when creating reply to non-existent parent")
    void createReply_WithNonExistentParent_ShouldThrowException() {
        // Given
        String appId = "APP_123";
        String parentCommentId = "COMMENT_NON_EXISTENT";
        when(commentRepository.findByAppIdAndCommentId(appId, parentCommentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.createReply(appId, parentCommentId, validCommentRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Parent comment not found");

        verify(commentRepository, times(1)).findByAppIdAndCommentId(appId, parentCommentId);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should get replies by parent ID with pagination")
    void getRepliesByParentId_WithValidParentId_ShouldReturnReplies() {
        // Given
        String parentCommentId = "COMMENT_PARENT_123";
        int page = 0;
        int size = 10;
        List<Comment> replies = Arrays.asList(validComment);
        when(commentRepository.findRepliesByParentId(parentCommentId, page, size)).thenReturn(replies);

        // When
        List<CommentResponse> result = commentService.getRepliesByParentId(parentCommentId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCommentId()).isEqualTo("COMMENT_123");

        verify(commentRepository, times(1)).findRepliesByParentId(parentCommentId, page, size);
    }

    @Test
    @DisplayName("Should search comments by app ID and keyword")
    void searchCommentsByAppId_WithValidParameters_ShouldReturnSearchResponse() {
        // Given
        String appId = "APP_123";
        String keyword = "test";
        int page = 0;
        int size = 10;
        List<Comment> searchResults = Arrays.asList(validComment);
        SearchResponse<Comment> searchResponse = SearchResponse.<Comment>builder()
                .items(searchResults)
                .totalElements(1L)
                .page(page)
                .size(size)
                .hasNext(false)
                .build();

        when(commentSearchRepository.searchCommentsByAppId(appId, keyword, page, size)).thenReturn(searchResponse);

        // When
        SearchResponse<CommentResponse> result = commentService.searchCommentsByAppId(appId, keyword, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getPage()).isEqualTo(page);
        assertThat(result.getSize()).isEqualTo(size);
        assertThat(result.getHasNext()).isFalse();

        verify(commentSearchRepository, times(1)).searchCommentsByAppId(appId, keyword, page, size);
    }

    @Test
    @DisplayName("Should get comments by user for app")
    void getCommentsByUserForApp_WithValidParameters_ShouldReturnComments() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        List<Comment> comments = Arrays.asList(validComment);
        when(commentRepository.findByAppIdAndUserId(appId, userId)).thenReturn(comments);

        // When
        List<CommentResponse> result = commentService.getCommentsByUserForApp(appId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("USER_123");

        verify(commentRepository, times(1)).findByAppIdAndUserId(appId, userId);
    }

    @Test
    @DisplayName("Should get top-level comments excluding user")
    void getTopLevelCommentsByAppIdExcludingUser_WithValidParameters_ShouldReturnComments() {
        // Given
        String appId = "APP_123";
        String userId = "USER_123";
        int page = 0;
        int size = 10;
        List<Comment> comments = Arrays.asList(validComment);
        when(commentRepository.findTopLevelCommentsByAppIdExcludingUser(appId, userId, page, size)).thenReturn(comments);

        // When
        List<CommentResponse> result = commentService.getTopLevelCommentsByAppIdExcludingUser(appId, userId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAppId()).isEqualTo("APP_123");

        verify(commentRepository, times(1)).findTopLevelCommentsByAppIdExcludingUser(appId, userId, page, size);
    }

    @Test
    @DisplayName("Should handle repository exception during comment creation")
    void createComment_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Given
        String appId = "APP_123";
        when(commentRepository.save(any(Comment.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(appId, validCommentRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should handle search repository exception during comment creation")
    void createComment_WhenSearchRepositoryThrowsException_ShouldPropagateException() {
        // Given
        String appId = "APP_123";
        when(commentRepository.save(any(Comment.class))).thenReturn(validComment);
        doThrow(new RuntimeException("Search error")).when(commentSearchRepository).indexComment(any(Comment.class));

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(appId, validCommentRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Search error");

        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(commentSearchRepository, times(1)).indexComment(any(Comment.class));
    }
}
