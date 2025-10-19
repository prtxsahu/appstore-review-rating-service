package com.example.appstore.deleted.dto;
// MOVED TO: com.example.appstore.rating.dto.RatingRequest
// This file has been moved to the new feature-based structure
// TODO: Remove this file after confirming the refactoring is complete

/*
package com.example.appstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating or updating a rating.
 */
/*
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingRequest {
    // not the best practice to expect user Id in request body, this should be done by JWT auth, but for the scope of this project we are leaving it here.

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Rating value is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer value;
}
*/