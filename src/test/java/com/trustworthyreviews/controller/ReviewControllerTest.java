package com.trustworthyreviews.controller;

import com.trustworthyreviews.security.SupabaseUser;
import com.trustworthyreviews.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    private void mockAuth(SupabaseUser user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void addReview_unauthenticated_returns401() {
        SecurityContextHolder.clearContext();
        ResponseEntity<?> res = reviewController.addReview(Map.of());
        assertEquals(401, res.getStatusCodeValue());
    }

    @Test
    void addReview_missingFields_returns400() {
        mockAuth(new SupabaseUser("1", "test@mail.com", Map.of()));
        ResponseEntity<?> res = reviewController.addReview(Map.of("rating", 5));
        assertEquals(400, res.getStatusCodeValue());
    }

    @Test
    void addReview_invalidRating_returns400() {
        mockAuth(new SupabaseUser("1", "test@mail.com", Map.of()));
        ResponseEntity<?> res = reviewController.addReview(Map.of("product_id", 1, "rating", 6));
        assertEquals(400, res.getStatusCodeValue());
    }

    @Test
    void addReview_success() {
        mockAuth(new SupabaseUser("1", "test@mail.com", Map.of()));
        Map<String, Object> mockResult = Map.of("id", 123, "rating", 5);
        when(reviewService.addReview(1, "1", 5, "Great!")).thenReturn(mockResult);

        ResponseEntity<?> res = reviewController.addReview(
                Map.of("product_id", 1, "rating", 5, "review_text", "Great!")
        );

        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockResult, res.getBody());
    }


    @Test
    void getProductReviews_success_authenticated() {
        mockAuth(new SupabaseUser("1", "test@mail.com", Map.of()));
        List<Map<String, Object>> mockReviews = List.of(Map.of("rating", 5));
        when(reviewService.getReviewsForProduct(1, "1", null)).thenReturn(mockReviews);

        ResponseEntity<?> res = reviewController.getProductReviews(1, null);
        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockReviews, res.getBody());
    }

    @Test
    void getProductReviews_success_unauthenticated() {
        SecurityContextHolder.clearContext();
        List<Map<String, Object>> mockReviews = List.of(Map.of("rating", 5));
        when(reviewService.getReviewsForProduct(1, null, null)).thenReturn(mockReviews);

        ResponseEntity<?> res = reviewController.getProductReviews(1, null);
        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockReviews, res.getBody());
    }


    @Test
    void getProductReviewSummary_success() {
        Map<String, Object> mockSummary = Map.of("average", 4.5, "count", 10);
        when(reviewService.getReviewSummary(1)).thenReturn(mockSummary);

        ResponseEntity<?> res = reviewController.getProductReviewSummary(1);
        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockSummary, res.getBody());
    }
}
