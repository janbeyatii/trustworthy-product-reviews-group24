package com.trustworthyreviews.controller;

import com.trustworthyreviews.security.SupabaseUser;
import com.trustworthyreviews.service.HystrixUserService;
import com.trustworthyreviews.service.UserService;
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

    @Autowired
    private UserService userService;
    /**
     * Returns information about the currently authenticated user.
     */
    @GetMapping("/whoami")
    public ResponseEntity<?> whoAmI() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if user is authenticated and is of type SupabaseUser
        if (authentication == null || !(authentication.getPrincipal() instanceof SupabaseUser user)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        // Return user details
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "metadata", user.getMetadata()
        ));
    }

    /**
     * Searches users based on a query string (typically email or username).
     */
    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam String query) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof SupabaseUser)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        // Return empty list if query is blank
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

    /**
     * Retrieves a specific user's profile by user ID.
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof SupabaseUser)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        try {
            Map<String, Object> user = hystrixUserService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("message", "User not found"));
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching user: " + e.getMessage()));
        }
    }

    /**
     * Retrieves the list of users that the currently authenticated user is following.
     */
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

    /**
     * Retrieves the list of users who follow the currently authenticated user.
     */
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

    /**
     * Get the list of the ten closest users by Jaccard distance
     */
    @GetMapping("/users/me/recommended")
    public ResponseEntity<?> getRecommendedUsers() {

        //TODO: Add a check for if Jaccard distance has already been calculated recently


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof SupabaseUser user)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        try {
            //return ResponseEntity.ok("Hello, World!");
            return ResponseEntity.ok(userService.getRecommendedUsers(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching recommend users: " + e.getMessage()));
        }
    }
}
