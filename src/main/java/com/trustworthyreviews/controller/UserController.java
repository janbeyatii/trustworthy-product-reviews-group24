package com.trustworthyreviews.controller;

import com.trustworthyreviews.security.SupabaseUser;
import com.trustworthyreviews.service.HystrixUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private HystrixUserService hystrixUserService;

    @GetMapping("/whoami")
    public ResponseEntity<?> whoAmI() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof SupabaseUser user)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "metadata", user.getMetadata()
        ));
    }

    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam String query) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof SupabaseUser)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        try {
            List<Map<String, Object>> users = hystrixUserService.searchUsers(query.trim());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error searching users: " + e.getMessage()));
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String viewerId = null;

        if (authentication != null && authentication.getPrincipal() instanceof SupabaseUser) {
            viewerId = ((SupabaseUser) authentication.getPrincipal()).getId();
        }

        try {
            // Fetch user profile with metrics (similarity, degree of separation) if viewer is authenticated
            Map<String, Object> user = hystrixUserService.getUserProfileWithMetrics(userId, viewerId);
            
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("message", "User not found"));
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching user: " + e.getMessage()));
        }
    }

    @GetMapping("/users/me/following")
    public ResponseEntity<?> getCurrentUserFollowing() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof SupabaseUser user)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        try {
            return ResponseEntity.ok(hystrixUserService.getFollowingForUser(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching following: " + e.getMessage()));
        }
    }

    @GetMapping("/users/me/followers")
    public ResponseEntity<?> getCurrentUserFollowers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof SupabaseUser user)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        try {
            return ResponseEntity.ok(hystrixUserService.getFollowersForUser(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching followers: " + e.getMessage()));
        }
    }

    @GetMapping("/users/me/similar")
    public ResponseEntity<?> getSimilarUsers(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0.1") double minSimilarity) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof SupabaseUser user)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        if (limit < 1 || limit > 50) {
            return ResponseEntity.status(400).body(Map.of("message", "Limit must be between 1 and 50"));
        }
        
        if (minSimilarity < 0.0 || minSimilarity > 1.0) {
            return ResponseEntity.status(400).body(Map.of("message", "minSimilarity must be between 0.0 and 1.0"));
        }

        try {
            List<Map<String, Object>> similarUsers = hystrixUserService.findSimilarUsers(
                    user.getId(), limit, minSimilarity);
            return ResponseEntity.ok(similarUsers);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error finding similar users: " + e.getMessage()));
        }
    }

    @GetMapping("/users/similarity/{userId}")
    public ResponseEntity<?> calculateSimilarity(@PathVariable String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof SupabaseUser user)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        try {
            double similarity = hystrixUserService.calculateCombinedJaccardSimilarity(
                    user.getId(), userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("user_id", userId);
            result.put("similarity", Math.round(similarity * 1000.0) / 1000.0);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error calculating similarity: " + e.getMessage()));
        }
    }

    @GetMapping("/users/most-followed")
    public ResponseEntity<?> getMostFollowedUsers(@RequestParam(defaultValue = "10") int limit) {
        if (limit < 1 || limit > 50) {
            return ResponseEntity.status(400).body(Map.of("message", "Limit must be between 1 and 50"));
        }

        try {
            List<Map<String, Object>> mostFollowed = hystrixUserService.getMostFollowedUsers(limit);
            return ResponseEntity.ok(mostFollowed);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching most followed users: " + e.getMessage()));
        }
    }
}
