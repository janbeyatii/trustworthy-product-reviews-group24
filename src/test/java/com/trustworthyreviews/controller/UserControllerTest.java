package com.trustworthyreviews.controller;

import com.trustworthyreviews.security.SupabaseUser;
import com.trustworthyreviews.service.HystrixUserService;
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

class UserControllerTest {

    @Mock
    private HystrixUserService hystrixUserService;

    @InjectMocks
    private UserController userController;

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
    void whoAmI_test() {
        SupabaseUser user = new SupabaseUser("1", "test@mail.com", Map.of());
        mockAuth(user);

        ResponseEntity<?> res = userController.whoAmI();
        assertEquals(200, res.getStatusCodeValue());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals("1", body.get("id"));
        assertEquals("test@mail.com", body.get("email"));

        // Unauthenticated
        SecurityContextHolder.clearContext();
        ResponseEntity<?> unauthRes = userController.whoAmI();
        assertEquals(401, unauthRes.getStatusCodeValue());
    }

    @Test
    void searchUsers_test() {
        SupabaseUser user = new SupabaseUser("50", "me@mail.com", Map.of());
        mockAuth(user);

        List<Map<String, Object>> mockUsers = List.of(Map.of("id", "11", "email", "a@mail.com"));
        when(hystrixUserService.searchUsers("a@mail.com")).thenReturn(mockUsers);

        ResponseEntity<?> res = userController.searchUsers("a@mail.com");
        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockUsers, res.getBody());

        // Empty query
        ResponseEntity<?> emptyRes = userController.searchUsers("   ");
        assertEquals(200, emptyRes.getStatusCodeValue());
        assertTrue(((List<?>) emptyRes.getBody()).isEmpty());

        // Unauthenticated
        SecurityContextHolder.clearContext();
        ResponseEntity<?> unauthRes = userController.searchUsers("x@mail.com");
        assertEquals(401, unauthRes.getStatusCodeValue());
    }

    @Test
    void getUserProfile_test() {
        SupabaseUser user = new SupabaseUser("10", "auth@test.com", Map.of());
        mockAuth(user);

        Map<String, Object> mockUser = Map.of("id", "999", "email", "target@mail.com", "name", "Target User");
        when(hystrixUserService.getUserProfileWithMetrics("999", "10")).thenReturn(mockUser);

        ResponseEntity<?> res = userController.getUserProfile("999");
        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockUser, res.getBody());

        // Not found
        when(hystrixUserService.getUserProfileWithMetrics("200", "10")).thenReturn(null);
        ResponseEntity<?> notFoundRes = userController.getUserProfile("200");
        assertEquals(404, notFoundRes.getStatusCodeValue());

        // Unauthenticated allowed
        SecurityContextHolder.clearContext();
        when(hystrixUserService.getUserProfileWithMetrics("777", null)).thenReturn(mockUser);
        ResponseEntity<?> unauthRes = userController.getUserProfile("777");
        assertEquals(200, unauthRes.getStatusCodeValue());
        assertEquals(mockUser, unauthRes.getBody());
    }

    @Test
    void following_test() {
        SupabaseUser user = new SupabaseUser("500", "testf@mail.com", Map.of());
        mockAuth(user);

        List<Map<String, Object>> mockData = List.of(Map.of("email", "test1@mail.com"), Map.of("email", "test2@mail.com"));
        when(hystrixUserService.getFollowingForUser("500")).thenReturn(mockData);

        ResponseEntity<?> res = userController.getCurrentUserFollowing();
        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockData, res.getBody());

        SecurityContextHolder.clearContext();
        ResponseEntity<?> unauthRes = userController.getCurrentUserFollowing();
        assertEquals(401, unauthRes.getStatusCodeValue());
    }

    @Test
    void followers_test() {
        SupabaseUser user = new SupabaseUser("600", "testf@mail.com", Map.of());
        mockAuth(user);

        List<Map<String, Object>> mockData = List.of(Map.of("email", "test1@mail.com"), Map.of("email", "test2@mail.com"));
        when(hystrixUserService.getFollowersForUser("600")).thenReturn(mockData);

        ResponseEntity<?> res = userController.getCurrentUserFollowers();
        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockData, res.getBody());

        SecurityContextHolder.clearContext();
        ResponseEntity<?> unauthRes = userController.getCurrentUserFollowers();
        assertEquals(401, unauthRes.getStatusCodeValue());
    }

    @Test
    void getSimilarUsers_test() {
        SupabaseUser user = new SupabaseUser("1000", "sim@mail.com", Map.of());
        mockAuth(user);

        List<Map<String, Object>> mockSimilarUsers = List.of(
                Map.of("id", "101", "email", "u1@mail.com", "similarity", 0.9),
                Map.of("id", "102", "email", "u2@mail.com", "similarity", 0.8)
        );

        when(hystrixUserService.findSimilarUsers("1000", 10, 0.1)).thenReturn(mockSimilarUsers);

        ResponseEntity<?> res = userController.getSimilarUsers(10, 0.1);
        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockSimilarUsers, res.getBody());

        // Invalid limit
        ResponseEntity<?> invalidLimit = userController.getSimilarUsers(0, 0.1);
        assertEquals(400, invalidLimit.getStatusCodeValue());

        // Invalid minSimilarity
        ResponseEntity<?> invalidSim = userController.getSimilarUsers(10, -0.5);
        assertEquals(400, invalidSim.getStatusCodeValue());

        // Unauthenticated
        SecurityContextHolder.clearContext();
        ResponseEntity<?> unauth = userController.getSimilarUsers(10, 0.1);
        assertEquals(401, unauth.getStatusCodeValue());
    }

    @Test
    void calculateSimilarity_test() {
        SupabaseUser user = new SupabaseUser("2000", "calc@mail.com", Map.of());
        mockAuth(user);

        when(hystrixUserService.calculateCombinedJaccardSimilarity("2000", "3000")).thenReturn(0.756);

        ResponseEntity<?> res = userController.calculateSimilarity("3000");
        assertEquals(200, res.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        assertEquals("3000", body.get("user_id"));
        assertEquals(0.756, body.get("similarity"));

        // Unauthenticated
        SecurityContextHolder.clearContext();
        ResponseEntity<?> unauth = userController.calculateSimilarity("3000");
        assertEquals(401, unauth.getStatusCodeValue());
    }
}
