package com.trustworthyreviews.controller;

import com.trustworthyreviews.security.SupabaseUser;
import com.trustworthyreviews.service.UserService;
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
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
        log("Mocks initialized, security cleared");
    }

    // Logging helpers
    private void log(String msg) {
        System.out.println(">>> " + msg);
    }

    private void logStart(String name) {
        System.out.println("\n=== Running " + name + " ===");
    }

    private void logData(Object data) {
        System.out.println("Data: " + data);
    }

    private void mockAuth(SupabaseUser user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        log("Authenticated as: " + user.getEmail());
    }


    // whoAmI
    @Test
    void whoAmI() {
        logStart("whoAmI test");

        // Authenticated
        SupabaseUser u = new SupabaseUser("1", "test@mail.com", Map.of());
        mockAuth(u);

        ResponseEntity<?> authRes = userController.whoAmI();
        logData(authRes);

        assertEquals(200, authRes.getStatusCodeValue());
        Map<?, ?> body = (Map<?, ?>) authRes.getBody();
        assertEquals("1", body.get("id"));
        assertEquals("test@mail.com", body.get("email"));

        // Unauthenticated
        SecurityContextHolder.clearContext();
        log("Auth cleared");

        ResponseEntity<?> unauthRes = userController.whoAmI();
        logData(unauthRes);

        assertEquals(401, unauthRes.getStatusCodeValue());
    }


    @Test
    void searchUsers() {
        logStart("searchUsers test");

        SupabaseUser user = new SupabaseUser("50", "me@mail.com", Map.of());
        mockAuth(user);

        // Exact email match
        List<Map<String, Object>> mockUsers = List.of(
                Map.of("id", "11", "email", "a@mail.com")
        );
        logData(mockUsers);

        when(userService.searchUsers("a@mail.com")).thenReturn(mockUsers);
        log("searchUsers(\"a@mail.com\")");

        ResponseEntity<?> result1 = userController.searchUsers("a@mail.com");
        logData(result1);

        assertEquals(200, result1.getStatusCodeValue());
        assertEquals(mockUsers, result1.getBody());
        assertEquals(1, ((List<?>) result1.getBody()).size());   // only 1 user returned
        assertEquals("a@mail.com", ((Map<?, ?>)((List<?>) result1.getBody()).get(0)).get("email"));

        // Empty query
        ResponseEntity<?> result2 = userController.searchUsers("   ");
        logData(result2);
        assertTrue(((List<?>) result2.getBody()).isEmpty());

        // Unauthenticated
        SecurityContextHolder.clearContext();
        log("Auth cleared");

        ResponseEntity<?> result3 = userController.searchUsers("x@mail.com");
        logData(result3);

        assertEquals(401, result3.getStatusCodeValue());
    }


    // getUserProfile
    @Test
    void getUserProfile() {
        logStart("getUserProfile test");

        SupabaseUser user = new SupabaseUser("10", "auth@test.com", Map.of());
        mockAuth(user);

        // Found
        Map<String, Object> mockUser = Map.of(
                "id", "999",
                "email", "target@mail.com",
                "name", "Target User"
        );
        logData(mockUser);

        when(userService.getUserById("999")).thenReturn(mockUser);
        log("getUserById(\"999\")");

        ResponseEntity<?> result1 = userController.getUserProfile("999");
        logData(result1);

        assertEquals(200, result1.getStatusCodeValue());
        assertEquals(mockUser, result1.getBody());

        // Not found
        when(userService.getUserById("200")).thenReturn(null);
        log("getUserById(\"200\")");

        ResponseEntity<?> result2 = userController.getUserProfile("200");
        logData(result2);

        assertEquals(404, result2.getStatusCodeValue());

        // Unauthenticated
        SecurityContextHolder.clearContext();
        log("Auth cleared");

        ResponseEntity<?> result3 = userController.getUserProfile("777");
        logData(result3);

        assertEquals(401, result3.getStatusCodeValue());
    }


    // following
    @Test
    void following() {
        logStart("following test");

        SupabaseUser user = new SupabaseUser("500", "testf@mail.com", Map.of());
        mockAuth(user);

        List<Map<String, Object>> mockData = List.of(
                Map.of("email", "test1@mail.com"),
                Map.of("email", "test2@mail.com")
        );
        logData(mockData);

        when(userService.getFollowingForUser("500")).thenReturn(mockData);
        log("getFollowingForUser(\"500\")");

        // Authenticated
        ResponseEntity<?> result1 = userController.getCurrentUserFollowing();
        logData(result1);

        assertEquals(200, result1.getStatusCodeValue());
        assertEquals(mockData, result1.getBody());

        // Unauthenticated
        SecurityContextHolder.clearContext();
        log("Auth cleared");

        ResponseEntity<?> result2 = userController.getCurrentUserFollowing();
        logData(result2);

        assertEquals(401, result2.getStatusCodeValue());
    }

    // followers
    @Test
    void followers() {
        logStart("followers test");

        SupabaseUser user = new SupabaseUser("600", "testf@mail.com", Map.of());
        mockAuth(user);

        List<Map<String, Object>> mockData = List.of(
                Map.of("email", "test1@mail.com"),
                Map.of("email", "test2@mail.com")
        );
        logData(mockData);

        when(userService.getFollowersForUser("600")).thenReturn(mockData);
        log("getFollowersForUser(\"600\")");

        // Authenticated
        ResponseEntity<?> result1 = userController.getCurrentUserFollowers();
        logData(result1);

        assertEquals(200, result1.getStatusCodeValue());
        assertEquals(mockData, result1.getBody());

        // Unauthenticated
        SecurityContextHolder.clearContext();
        log("Auth cleared");

        ResponseEntity<?> result2 = userController.getCurrentUserFollowers();
        logData(result2);

        assertEquals(401, result2.getStatusCodeValue());
    }
}
