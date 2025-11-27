package com.trustworthyreviews.controller;

import com.trustworthyreviews.security.SupabaseUser;
import com.trustworthyreviews.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/reviews")
    public ResponseEntity<?> addReview(@RequestBody Map<String, Object> reviewData) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SupabaseUser user)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        try {
            Integer productId = (Integer) reviewData.get("product_id");
            Integer rating = (Integer) reviewData.get("rating");
            String reviewText = (String) reviewData.get("review_text");

            if (productId == null || rating == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "product_id and rating are required"));
            }

            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of("message", "Rating must be between 1 and 5"));
            }

            Map<String, Object> result = reviewService.addReview(productId, user.getId(), rating, reviewText);
            return ResponseEntity.ok(result);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error adding review: " + e.getMessage()));
        }
    }

    @GetMapping("/products/{id}/reviews")
    public ResponseEntity<?> getProductReviews(
            @PathVariable("id") int productId,
            @RequestParam(value = "sort", required = false) String sortBy) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = null;
        if (authentication != null && authentication.getPrincipal() instanceof SupabaseUser user) {
            currentUserId = user.getId();
        }

        try {
            List<Map<String, Object>> reviews = reviewService.getReviewsForProduct(productId, currentUserId, sortBy);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching reviews: " + e.getMessage()));
        }
    }

    @GetMapping("/products/{id}/summary")
    public ResponseEntity<?> getProductReviewSummary(@PathVariable("id") int productId) {
        try {
            Map<String, Object> summary = reviewService.getReviewSummary(productId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching review summary: " + e.getMessage()));
        }
    }
}

