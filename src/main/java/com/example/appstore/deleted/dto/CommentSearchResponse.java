package com.example.appstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for comment search results (preview data from Elasticsearch).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentSearchResponse {

    private String commentId;
    private String appId;
    private String parentId;
    private String text;
    private Instant createdAt;
    private Long subCommentCount;
    private Long likesCount;
    private Long dislikesCount;
}
