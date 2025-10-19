package com.example.appstore.comment.controller;

import com.example.appstore.comment.dto.CommentRequest;
import com.example.appstore.comment.dto.CommentResponse;
import com.example.appstore.comment.dto.UpdateCommentRequest;
import com.example.appstore.comment.service.CommentServiceInterface;
import com.example.appstore.shared.dto.PaginatedResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentServiceInterface commentService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String APP_ID = "APP_123";
    private static final String COMMENT_ID = "COMMENT_456";
    private static final String USER_ID = "USER_789";

    private CommentResponse sampleCommentResponse;
    private CommentRequest sampleCommentRequest;
    private UpdateCommentRequest sampleUpdateRequest;
    private PaginatedResult<CommentResponse> samplePaginatedResult;

    @BeforeEach
    void setUp() {
        // Setup sample comment response
        sampleCommentResponse = CommentResponse.builder()
                .commentId(COMMENT_ID)
                .appId(APP_ID)
                .userId(USER_ID)
                .parentId(null)
                .text("Test comment")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .subCommentCount(0L)
                .likesCount(0L)
                .dislikesCount(0L)
                .build();

        // Setup sample comment request
        sampleCommentRequest = CommentRequest.builder()
                .userId(USER_ID)
                .text("Test comment")
                .build();

        // Setup sample update request
        sampleUpdateRequest = UpdateCommentRequest.builder()
                .text("Updated comment")
                .build();

        // Setup sample paginated result
        samplePaginatedResult = PaginatedResult.<CommentResponse>builder()
                .items(List.of(sampleCommentResponse))
                .lastEvaluatedKey(null)
                .build();
    }

    @Test
    void createComment_shouldReturnCreatedComment() throws Exception {
        // Given
        when(commentService.createComment(eq(APP_ID), any(CommentRequest.class)))
                .thenReturn(sampleCommentResponse);

        // When & Then
        mockMvc.perform(post("/apps/{appId}/comments", APP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCommentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value(COMMENT_ID))
                .andExpect(jsonPath("$.appId").value(APP_ID))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.text").value("Test comment"));
    }

    @Test
    void createComment_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        // Given - invalid request (missing required fields)
        CommentRequest invalidRequest = CommentRequest.builder()
                .text("") // Empty text should fail validation
                .build();

        // When & Then
        mockMvc.perform(post("/apps/{appId}/comments", APP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getComments_withoutCursor_shouldReturnFirstPage() throws Exception {
        // Given
        when(commentService.getTopLevelCommentsByAppIdWithCursor(eq(APP_ID), isNull(), eq(10)))
                .thenReturn(samplePaginatedResult);

        // When & Then
        mockMvc.perform(get("/apps/{appId}/comments", APP_ID)
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].commentId").value(COMMENT_ID))
                .andExpect(jsonPath("$.hasNextPage").value(false))
                .andExpect(jsonPath("$.nextPageCursor").isEmpty());
    }

    @Test
    void getComments_withCursor_shouldReturnNextPage() throws Exception {
        // Given
        String cursor = "COMMENT_123";
        when(commentService.getTopLevelCommentsByAppIdWithCursor(eq(APP_ID), eq(cursor), eq(5)))
                .thenReturn(samplePaginatedResult);

        // When & Then
        mockMvc.perform(get("/apps/{appId}/comments", APP_ID)
                        .param("cursor", cursor)
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].commentId").value(COMMENT_ID));
    }

    @Test
    void getComments_withDefaultParameters_shouldUseDefaults() throws Exception {
        // Given
        when(commentService.getTopLevelCommentsByAppIdWithCursor(eq(APP_ID), isNull(), eq(10)))
                .thenReturn(samplePaginatedResult);

        // When & Then
        mockMvc.perform(get("/apps/{appId}/comments", APP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void updateComment_shouldReturnUpdatedComment() throws Exception {
        // Given
        CommentResponse updatedResponse = CommentResponse.builder()
                .commentId(COMMENT_ID)
                .appId(APP_ID)
                .userId(USER_ID)
                .parentId(null)
                .text("Updated comment")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .subCommentCount(0L)
                .likesCount(0L)
                .dislikesCount(0L)
                .build();

        when(commentService.updateComment(eq(APP_ID), eq(COMMENT_ID), any(UpdateCommentRequest.class)))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/apps/{appId}/comments/{commentId}", APP_ID, COMMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(COMMENT_ID))
                .andExpect(jsonPath("$.text").value("Updated comment"));
    }

    @Test
    void updateComment_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        // Given - invalid request (empty text)
        UpdateCommentRequest invalidRequest = UpdateCommentRequest.builder()
                .text("") // Empty text should fail validation
                .build();

        // When & Then
        mockMvc.perform(put("/apps/{appId}/comments/{commentId}", APP_ID, COMMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteComment_shouldReturnNoContent() throws Exception {
        // Given - deleteComment is void, so we don't need to mock return value
        // The service will throw exception if comment not found

        // When & Then
        mockMvc.perform(delete("/apps/{appId}/comments/{commentId}", APP_ID, COMMENT_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteComment_whenCommentNotFound_shouldReturnNotFound() throws Exception {
        // Given
        doThrow(new com.example.appstore.shared.exception.ResourceNotFoundException("Comment not found"))
                .when(commentService).deleteComment(eq(APP_ID), eq(COMMENT_ID));

        // When & Then
        mockMvc.perform(delete("/apps/{appId}/comments/{commentId}", APP_ID, COMMENT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void likeComment_shouldReturnUpdatedComment() throws Exception {
        // Given
        CommentResponse likedResponse = CommentResponse.builder()
                .commentId(COMMENT_ID)
                .appId(APP_ID)
                .userId(USER_ID)
                .parentId(null)
                .text("Test comment")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .subCommentCount(0L)
                .likesCount(1L) // Increased likes
                .dislikesCount(0L)
                .build();

        when(commentService.likeComment(eq(APP_ID), eq(COMMENT_ID)))
                .thenReturn(likedResponse);

        // When & Then
        mockMvc.perform(post("/apps/{appId}/comments/{commentId}/like", APP_ID, COMMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(COMMENT_ID))
                .andExpect(jsonPath("$.likesCount").value(1));
    }

    @Test
    void dislikeComment_shouldReturnUpdatedComment() throws Exception {
        // Given
        CommentResponse dislikedResponse = CommentResponse.builder()
                .commentId(COMMENT_ID)
                .appId(APP_ID)
                .userId(USER_ID)
                .parentId(null)
                .text("Test comment")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .subCommentCount(0L)
                .likesCount(0L)
                .dislikesCount(1L) // Increased dislikes
                .build();

        when(commentService.dislikeComment(eq(APP_ID), eq(COMMENT_ID)))
                .thenReturn(dislikedResponse);

        // When & Then
        mockMvc.perform(post("/apps/{appId}/comments/{commentId}/dislike", APP_ID, COMMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(COMMENT_ID))
                .andExpect(jsonPath("$.dislikesCount").value(1));
    }

    @Test
    void createReply_shouldReturnCreatedReply() throws Exception {
        // Given
        String parentId = "PARENT_123";
        CommentRequest replyRequest = CommentRequest.builder()
                .userId(USER_ID)
                .text("Reply to comment")
                .build();

        CommentResponse replyResponse = CommentResponse.builder()
                .commentId("REPLY_456")
                .appId(APP_ID)
                .userId(USER_ID)
                .parentId(parentId)
                .text("Reply to comment")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .subCommentCount(0L)
                .likesCount(0L)
                .dislikesCount(0L)
                .build();

        when(commentService.createReply(eq(APP_ID), eq(parentId), any(CommentRequest.class)))
                .thenReturn(replyResponse);

        // When & Then
        mockMvc.perform(post("/apps/{appId}/comments/{parentId}/replies", APP_ID, parentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value("REPLY_456"))
                .andExpect(jsonPath("$.parentId").value(parentId))
                .andExpect(jsonPath("$.text").value("Reply to comment"));
    }

    @Test
    void getReplies_shouldReturnRepliesList() throws Exception {
        // Given
        String parentId = "PARENT_123";
        List<CommentResponse> replies = List.of(
                CommentResponse.builder()
                        .commentId("REPLY_1")
                        .appId(APP_ID)
                        .userId(USER_ID)
                        .parentId(parentId)
                        .text("Reply 1")
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .subCommentCount(0L)
                        .likesCount(0L)
                        .dislikesCount(0L)
                        .build()
        );

        when(commentService.getRepliesByParentId(eq(parentId), eq(0), eq(10)))
                .thenReturn(replies);

        // When & Then
        mockMvc.perform(get("/apps/{appId}/comments/{parentId}/replies", APP_ID, parentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].commentId").value("REPLY_1"))
                .andExpect(jsonPath("$.comments[0].parentId").value(parentId))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

}
