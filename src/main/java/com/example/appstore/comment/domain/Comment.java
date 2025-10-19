package com.example.appstore.comment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

/**
 * Comment entity representing a comment or reply on an app.
 * Composite key: app_id (PK) + comment_id (SK)
 * GSI: parent_index on parent_id for threaded comments
 * Note: deleted field removed as per updated design.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Comment {

    private String appId;
    private String commentId;
    private String userId;
    private String parentId;
    private String text;
    private Instant createdAt;
    private Instant updatedAt;
    private Long subCommentCount;
    private Long likesCount;
    private Long dislikesCount;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("app_id")
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("comment_id")
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    @DynamoDbAttribute("user_id")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbAttribute("parent_id")
    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @DynamoDbAttribute("text")
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @DynamoDbAttribute("created_at")
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("updated_at")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @DynamoDbAttribute("sub_comment_count")
    public Long getSubCommentCount() {
        return subCommentCount != null ? subCommentCount : 0L;
    }

    public void setSubCommentCount(Long subCommentCount) {
        this.subCommentCount = subCommentCount;
    }

    @DynamoDbAttribute("likes_count")
    public Long getLikesCount() {
        return likesCount != null ? likesCount : 0L;
    }

    public void setLikesCount(Long likesCount) {
        this.likesCount = likesCount;
    }

    @DynamoDbAttribute("dislikes_count")
    public Long getDislikesCount() {
        return dislikesCount != null ? dislikesCount : 0L;
    }

    public void setDislikesCount(Long dislikesCount) {
        this.dislikesCount = dislikesCount;
    }
}
