package com.example.appstore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new app.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppRequest {

    @NotBlank(message = "App name is required")
    @Size(min = 1, max = 200, message = "App name must be between 1 and 200 characters")
    private String name;

    @NotBlank(message = "App description is required")
    @Size(min = 1, max = 1000, message = "App description must be between 1 and 1000 characters")
    private String description;
}
