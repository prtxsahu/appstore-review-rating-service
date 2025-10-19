package com.example.appstore.app.controller;

import com.example.appstore.app.dto.*;
import com.example.appstore.app.service.AppServiceInterface;
import com.example.appstore.shared.dto.SearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppController.class)
class AppControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppServiceInterface appService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String APP_ID = "APP_123";
    private static final String USER_ID = "USER_789";

    private AppResponse sampleAppResponse;
    private CreateAppRequest sampleCreateRequest;
    private AppSearchResponse sampleSearchResponse;

    @BeforeEach
    void setUp() {
        // Setup sample app response
        sampleAppResponse = AppResponse.builder()
                .appId(APP_ID)
                .name("Test App")
                .description("A test application")
                .avgRating(BigDecimal.valueOf(4.5))
                .updatedAt(Instant.now())
                .userRating(null)
                .userComments(null)
                .comments(null)
                .commentPagination(null)
                .build();

        // Setup sample create request
        sampleCreateRequest = CreateAppRequest.builder()
                .name("Test App")
                .description("A test application")
                .build();

        // Setup sample search response
        sampleSearchResponse = AppSearchResponse.builder()
                .appId(APP_ID)
                .name("Test App")
                .description("A test application")
                .avgRating(BigDecimal.valueOf(4.5))
                .updatedAt(Instant.now())
                .build();

    }

    @Test
    void createApp_shouldReturnCreatedApp() throws Exception {
        // Given
        when(appService.createApp(any(CreateAppRequest.class)))
                .thenReturn(sampleAppResponse);

        // When & Then
        mockMvc.perform(post("/apps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appId").value(APP_ID))
                .andExpect(jsonPath("$.name").value("Test App"))
                .andExpect(jsonPath("$.description").value("A test application"))
                .andExpect(jsonPath("$.avgRating").value(4.5));
    }

    @Test
    void createApp_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        // Given - invalid request (missing required fields)
        CreateAppRequest invalidRequest = CreateAppRequest.builder()
                .name("") // Empty name should fail validation
                .description("Valid description")
                .build();

        // When & Then
        mockMvc.perform(post("/apps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAppBasicInfo_shouldReturnAppInfo() throws Exception {
        // Given
        when(appService.getAppBasicInfo(eq(APP_ID)))
                .thenReturn(sampleAppResponse);

        // When & Then
        mockMvc.perform(get("/apps/{appId}", APP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value(APP_ID))
                .andExpect(jsonPath("$.name").value("Test App"))
                .andExpect(jsonPath("$.description").value("A test application"));
    }

    @Test
    void getAppBasicInfo_whenAppNotFound_shouldReturnNotFound() throws Exception {
        // Given
        when(appService.getAppBasicInfo(eq("NONEXISTENT_APP")))
                .thenThrow(new com.example.appstore.shared.exception.ResourceNotFoundException("App not found"));

        // When & Then
        mockMvc.perform(get("/apps/{appId}", "NONEXISTENT_APP"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAppById_withUserId_shouldReturnAppWithUserData() throws Exception {
        // Given
        AppResponse appWithUserData = AppResponse.builder()
                .appId(APP_ID)
                .name("Test App")
                .description("A test application")
                .avgRating(BigDecimal.valueOf(4.5))
                .updatedAt(Instant.now())
                .userRating(5) // User's rating
                .userComments(List.of()) // User's comments
                .comments(List.of()) // All comments
                .commentPagination(null)
                .build();

        when(appService.getAppById(eq(APP_ID), eq(USER_ID)))
                .thenReturn(appWithUserData);

        // When & Then
        mockMvc.perform(get("/apps/{appId}/detailed", APP_ID)
                        .param("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appId").value(APP_ID))
                .andExpect(jsonPath("$.userRating").value(5))
                .andExpect(jsonPath("$.userComments").isArray())
                .andExpect(jsonPath("$.comments").isArray());
    }

    @Test
    void searchApps_shouldReturnSearchResults() throws Exception {
        // Given
        String keyword = "test";
        when(appService.searchApps(eq(keyword), eq(0), eq(20)))
                .thenReturn(List.of(sampleAppResponse));

        // When & Then
        mockMvc.perform(get("/apps")
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].appId").value(APP_ID))
                .andExpect(jsonPath("$.items[0].name").value("Test App"));
    }

    @Test
    void searchApps_withCustomPagination_shouldReturnPaginatedResults() throws Exception {
        // Given
        String keyword = "test";

        when(appService.searchApps(eq(keyword), eq(1), eq(5)))
                .thenReturn(List.of(sampleAppResponse));

        // When & Then
        mockMvc.perform(get("/apps")
                        .param("keyword", keyword)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void searchApps_withMissingKeyword_shouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/apps"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchApps_withEmptyKeyword_shouldReturnOk() throws Exception {
        // Given - empty keyword is accepted by the controller
        when(appService.searchApps(eq(""), eq(0), eq(20)))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/apps")
                        .param("keyword", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void searchApps_withEmptyResults_shouldReturnEmptyList() throws Exception {
        // Given
        when(appService.searchApps(eq("nonexistent"), eq(0), eq(10)))
                .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/apps")
                        .param("keyword", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void searchApps_withNegativePage_shouldUseDefaultPage() throws Exception {
        // Given
        String keyword = "test";
        when(appService.searchApps(eq(keyword), eq(-1), eq(10)))
                .thenReturn(List.of(sampleAppResponse));

        // When & Then
        mockMvc.perform(get("/apps")
                        .param("keyword", keyword)
                        .param("page", "-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void searchApps_withZeroSize_shouldUseDefaultSize() throws Exception {
        // Given
        String keyword = "test";
        when(appService.searchApps(eq(keyword), eq(0), eq(0)))
                .thenReturn(List.of(sampleAppResponse));

        // When & Then
        mockMvc.perform(get("/apps")
                        .param("keyword", keyword)
                        .param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void searchApps_withSpecialCharacters_shouldHandleCorrectly() throws Exception {
        // Given
        String specialKeyword = "test@#$%^&*()";
        when(appService.searchApps(eq(specialKeyword), eq(0), eq(10)))
                .thenReturn(List.of(sampleAppResponse));

        // When & Then
        mockMvc.perform(get("/apps")
                        .param("keyword", specialKeyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void searchApps_withLongKeyword_shouldHandleCorrectly() throws Exception {
        // Given
        String longKeyword = "a".repeat(1000); // Very long keyword
        when(appService.searchApps(eq(longKeyword), eq(0), eq(10)))
                .thenReturn(List.of(sampleAppResponse));

        // When & Then
        mockMvc.perform(get("/apps")
                        .param("keyword", longKeyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

}
