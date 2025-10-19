package com.example.appstore.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a comment or reply.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Comment text is required")
    @Size(min = 1, max = 2000, message = "Comment text must be between 1 and 2000 characters")
    private String text;

    // Optional: for replies, this will be the parent comment ID
    private String parentId;
}
