package com.trustworthyreviews.controller;

import com.trustworthyreviews.security.SupabaseUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/whoami")
public class UserController {

    @GetMapping
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
}
