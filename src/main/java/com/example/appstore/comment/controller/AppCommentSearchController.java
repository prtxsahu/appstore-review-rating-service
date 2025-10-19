package com.example.appstore.comment.controller;

import com.example.appstore.comment.dto.CommentSearchResponse;
import com.example.appstore.shared.dto.SearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Controller for app-specific comment search operations.
 * Currently returns dummy responses - will be connected to service layer later.
 */
@Slf4j
@RestController
@RequestMapping("/apps/{appId}/comments")
public class AppCommentSearchController {

    /**
     * Search comments within a specific app by keyword.
     * GET /apps/{appId}/comments/search?keyword={keyword}&page={page}&size={size}
     */
    @GetMapping("/search")
    public ResponseEntity<SearchResponse<CommentSearchResponse>> searchAppComments(
            @PathVariable String appId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        
        log.info("Searching comments in app {} with keyword: {}, page: {}, size: {}", 
                appId, keyword, page, size);
        
        // TODO: Connect to CommentService and Elasticsearch (filtered by appId)
        CommentSearchResponse comment1 = CommentSearchResponse.builder()
                .commentId("COMMENT_123")
                .appId(appId)
                .parentId(null)
                .text("This app is amazing! " + keyword)
                .createdAt(Instant.now())
                .build();
        
        CommentSearchResponse comment2 = CommentSearchResponse.builder()
                .commentId("COMMENT_456")
                .appId(appId)
                .parentId("COMMENT_123")
                .text("I agree with the " + keyword + " comment!")
                .createdAt(Instant.now())
                .build();
        
        SearchResponse<CommentSearchResponse> response = SearchResponse.<CommentSearchResponse>builder()
                .items(List.of(comment1, comment2))
                .page(page)
                .size(size)
                .totalElements(2L)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        
        return ResponseEntity.ok(response);
    }
}
