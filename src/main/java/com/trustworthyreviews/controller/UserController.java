package com.trustworthyreviews.controller;

import com.trustworthyreviews.security.SupabaseUser;
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
    private UserService userService;

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
            List<Map<String, Object>> users = userService.searchUsers(query.trim());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error searching users: " + e.getMessage()));
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SupabaseUser)) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }

        try {
            Map<String, Object> user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("message", "User not found"));
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching user: " + e.getMessage()));
        }
    }
}
