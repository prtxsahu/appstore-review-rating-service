package com.example.appstore.comment.controller;

import com.example.appstore.comment.dto.CommentResponse;
import com.example.appstore.comment.service.CommentServiceInterface;
import com.example.appstore.shared.dto.SearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentSearchController.class)
class CommentSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentServiceInterface commentService;

    private static final String APP_ID = "APP_123";
    private static final String KEYWORD = "test";
    private static final String COMMENT_ID = "COMMENT_456";
    private static final String USER_ID = "USER_789";

    private CommentResponse sampleCommentResponse;
    private SearchResponse<CommentResponse> sampleSearchResponse;

    @BeforeEach
    void setUp() {
        // Setup sample comment response
        sampleCommentResponse = CommentResponse.builder()
                .commentId(COMMENT_ID)
                .appId(APP_ID)
                .userId(USER_ID)
                .parentId(null)
                .text("This is a test comment")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .subCommentCount(0L)
                .likesCount(0L)
                .dislikesCount(0L)
                .build();

        // Setup sample search response
        sampleSearchResponse = SearchResponse.<CommentResponse>builder()
                .items(List.of(sampleCommentResponse))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    @Test
    void searchComments_shouldReturnSearchResults() throws Exception {
        // Given
        when(commentService.searchCommentsByAppId(eq(APP_ID), eq(KEYWORD), eq(0), eq(20)))
                .thenReturn(sampleSearchResponse);

        // When & Then
        mockMvc.perform(get("/comments/search")
                        .param("appId", APP_ID)
                        .param("keyword", KEYWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].commentId").value(COMMENT_ID))
                .andExpect(jsonPath("$.items[0].text").value("This is a test comment"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void searchComments_withCustomPagination_shouldReturnPaginatedResults() throws Exception {
        // Given
        SearchResponse<CommentResponse> paginatedResponse = SearchResponse.<CommentResponse>builder()
                .items(List.of(sampleCommentResponse))
                .page(1)
                .size(10)
                .totalElements(25L)
                .totalPages(3)
                .hasNext(true)
                .hasPrevious(true)
                .build();

        when(commentService.searchCommentsByAppId(eq(APP_ID), eq(KEYWORD), eq(1), eq(10)))
                .thenReturn(paginatedResponse);

        // When & Then
        mockMvc.perform(get("/comments/search")
                        .param("appId", APP_ID)
                        .param("keyword", KEYWORD)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(true));
    }

    @Test
    void searchComments_withEmptyResults_shouldReturnEmptyList() throws Exception {
        // Given
        SearchResponse<CommentResponse> emptyResponse = SearchResponse.<CommentResponse>builder()
                .items(List.of())
                .page(0)
                .size(20)
                .totalElements(0L)
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(commentService.searchCommentsByAppId(eq(APP_ID), eq("nonexistent"), eq(0), eq(20)))
                .thenReturn(emptyResponse);

        // When & Then
        mockMvc.perform(get("/comments/search")
                        .param("appId", APP_ID)
                        .param("keyword", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void searchComments_withMissingAppId_shouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/comments/search")
                        .param("keyword", KEYWORD))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchComments_withMissingKeyword_shouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/comments/search")
                        .param("appId", APP_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchComments_withEmptyKeyword_shouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/comments/search")
                        .param("appId", APP_ID)
                        .param("keyword", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchComments_withNegativePage_shouldUseDefaultPage() throws Exception {
        // Given
        when(commentService.searchCommentsByAppId(eq(APP_ID), eq(KEYWORD), eq(-1), eq(20)))
                .thenReturn(sampleSearchResponse);

        // When & Then
        mockMvc.perform(get("/comments/search")
                        .param("appId", APP_ID)
                        .param("keyword", KEYWORD)
                        .param("page", "-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void searchComments_withZeroSize_shouldUseDefaultSize() throws Exception {
        // Given
        when(commentService.searchCommentsByAppId(eq(APP_ID), eq(KEYWORD), eq(0), eq(0)))
                .thenReturn(sampleSearchResponse);

        // When & Then
        mockMvc.perform(get("/comments/search")
                        .param("appId", APP_ID)
                        .param("keyword", KEYWORD)
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void searchComments_withLargeSize_shouldReturnResults() throws Exception {
        // Given
        SearchResponse<CommentResponse> largeResponse = SearchResponse.<CommentResponse>builder()
                .items(List.of(sampleCommentResponse))
                .page(0)
                .size(100)
                .totalElements(1L)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(commentService.searchCommentsByAppId(eq(APP_ID), eq(KEYWORD), eq(0), eq(100)))
                .thenReturn(largeResponse);

        // When & Then
        mockMvc.perform(get("/comments/search")
                        .param("appId", APP_ID)
                        .param("keyword", KEYWORD)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void searchComments_withSpecialCharacters_shouldHandleCorrectly() throws Exception {
        // Given
        String specialKeyword = "test@#$%^&*()";
        when(commentService.searchCommentsByAppId(eq(APP_ID), eq(specialKeyword), eq(0), eq(20)))
                .thenReturn(sampleSearchResponse);

        // When & Then
        mockMvc.perform(get("/comments/search")
                        .param("appId", APP_ID)
                        .param("keyword", specialKeyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void searchComments_withLongKeyword_shouldHandleCorrectly() throws Exception {
        // Given
        String longKeyword = "a".repeat(1000); // Very long keyword
        when(commentService.searchCommentsByAppId(eq(APP_ID), eq(longKeyword), eq(0), eq(20)))
                .thenReturn(sampleSearchResponse);

        // When & Then
        mockMvc.perform(get("/comments/search")
                        .param("appId", APP_ID)
                        .param("keyword", longKeyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }
}
