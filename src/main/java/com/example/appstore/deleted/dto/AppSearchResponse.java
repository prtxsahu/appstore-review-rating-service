package com.example.appstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for app search results (preview data from Elasticsearch).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppSearchResponse {

    private String appId;
    private String name;
    private String description;
    private BigDecimal avgRating;
    private Instant updatedAt;
}
