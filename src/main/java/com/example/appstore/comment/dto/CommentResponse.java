package com.example.appstore.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for comment data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private String commentId;
    private String appId;
    private String userId;
    private String parentId;
    private String text;
    private Instant createdAt;
    private Instant updatedAt;
    private Long subCommentCount;
    private Long likesCount;
    private Long dislikesCount;
}
