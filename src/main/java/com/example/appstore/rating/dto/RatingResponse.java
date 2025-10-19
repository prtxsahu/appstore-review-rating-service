package com.example.appstore.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for rating data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {

    private String appId;
    private String userId;
    private Integer value;
    private Instant createdAt;
    private Instant updatedAt;
}
