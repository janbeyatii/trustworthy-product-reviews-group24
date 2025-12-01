package com.trustworthyreviews.controller;
import com.trustworthyreviews.TestSecurityConfig;

import com.trustworthyreviews.security.SupabaseUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)  
@Import(TestSecurityConfig.class)

public class ReviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ----------------------------------------------------
    // Helper
    // ----------------------------------------------------
    private void loginAsRealUser(String id, String email, Map<String, Object> metadata) {
        SupabaseUser user = new SupabaseUser(id, email, metadata);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ----------------------------------------------------
    // Tests
    // ----------------------------------------------------

    @Test
    public void addReview_unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":1,\"rating\":5,\"review_text\":\"Great!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void addReview_realUser_handlesDuplicates() throws Exception {

        loginAsRealUser(
                "490a2cba-3170-495d-9991-3e5855a7f00d",
                "testm@gmail.com",
                Map.of("display_name", "Test-M")
        );

        String json = """
            {"product_id":1,"rating":5,"review_text":"Excellent product!"}
        """;

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    if (code != 200 && code != 409) {
                        throw new AssertionError("Expected 200 or 409, got: " + code);
                    }
                });
    }

    @Test
    public void getProductReviews_unauthorized_allowed() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/products/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getProductReviews_realUser() throws Exception {

        loginAsRealUser(
                "490a2cba-3170-495d-9991-3e5855a7f00d",
                "testm@gmail.com",
                Map.of("display_name", "Test-M")
        );

        mockMvc.perform(get("/api/products/1/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getProductReviewSummary() throws Exception {
        mockMvc.perform(get("/api/products/1/summary"))
                .andExpect(status().isOk());
    }
}
