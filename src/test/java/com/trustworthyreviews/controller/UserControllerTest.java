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
    }

    private void mockAuth(SupabaseUser user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        System.out.println("Authenticated as: " + user.getEmail());
    }

    @Test
    void whoAmI_test() {
        System.out.println("Running whoAmI test");

        SupabaseUser user = new SupabaseUser("1", "test@mail.com", Map.of());
        mockAuth(user);

        ResponseEntity<?> res = userController.whoAmI();
        System.out.println("Mock service (authenticated user) result: " + res.getBody());

        assertEquals(200, res.getStatusCodeValue());
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertEquals("1", body.get("id"));
        assertEquals("test@mail.com", body.get("email"));

        // Unauthenticated
        SecurityContextHolder.clearContext();
        System.out.println("Authentication cleared");

        ResponseEntity<?> unauthRes = userController.whoAmI();
        System.out.println("Mock service (unauthenticated) result: " + unauthRes.getBody());

        assertEquals(401, unauthRes.getStatusCodeValue());
        System.out.println("whoAmI test completed\n");
    }

    @Test
    void searchUsers_test() {
        System.out.println("Running searchUsers test");

        SupabaseUser user = new SupabaseUser("50", "me@mail.com", Map.of());
        mockAuth(user);

        List<Map<String, Object>> mockUsers = List.of(Map.of("id", "11", "email", "a@mail.com"));
        when(userService.searchUsers("a@mail.com")).thenReturn(mockUsers);

        System.out.println("Query: 'a@mail.com'");
        System.out.println("Mock service will return: " + mockUsers);

        ResponseEntity<?> res = userController.searchUsers("a@mail.com");
        System.out.println("Result: " + res.getBody());

        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockUsers, res.getBody());

        // Empty query
        ResponseEntity<?> emptyRes = userController.searchUsers("   ");
        System.out.println("Query: '   ' (empty)");
        System.out.println("Result: " + emptyRes.getBody());

        assertEquals(200, emptyRes.getStatusCodeValue());
        assertTrue(((List<?>) emptyRes.getBody()).isEmpty());

        // Unauthenticated
        SecurityContextHolder.clearContext();
        System.out.println("Authentication cleared");

        ResponseEntity<?> unauthRes = userController.searchUsers("x@mail.com");
        System.out.println("Query: 'x@mail.com' (unauthenticated)");
        System.out.println("Result: " + unauthRes.getBody());

        assertEquals(401, unauthRes.getStatusCodeValue());
        System.out.println("searchUsers test completed\n");
    }

    @Test
    void getUserProfile_test() {
        System.out.println("Running getUserProfile test");

        SupabaseUser user = new SupabaseUser("10", "auth@test.com", Map.of());
        mockAuth(user);

        Map<String, Object> mockUser = Map.of("id", "999", "email", "target@mail.com", "name", "Target User");
        when(userService.getUserById("999")).thenReturn(mockUser);

        System.out.println("Query: getUserProfile('999')");
        System.out.println("Mock service will return: " + mockUser);

        ResponseEntity<?> res = userController.getUserProfile("999");
        System.out.println("Result: " + res.getBody());

        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockUser, res.getBody());

        // Not found
        when(userService.getUserById("200")).thenReturn(null);
        ResponseEntity<?> notFoundRes = userController.getUserProfile("200");
        System.out.println("Query: getUserProfile('200')");
        System.out.println("Result: " + notFoundRes.getBody());

        assertEquals(404, notFoundRes.getStatusCodeValue());

        // Unauthenticated
        SecurityContextHolder.clearContext();
        ResponseEntity<?> unauthRes = userController.getUserProfile("777");
        System.out.println("Query: getUserProfile('777') (unauthenticated)");
        System.out.println("Result: " + unauthRes.getBody());

        assertEquals(401, unauthRes.getStatusCodeValue());
        System.out.println("getUserProfile test completed\n");
    }

    @Test
    void following_test() {
        System.out.println("Running following test");

        SupabaseUser user = new SupabaseUser("500", "testf@mail.com", Map.of());
        mockAuth(user);

        List<Map<String, Object>> mockData = List.of(Map.of("email", "test1@mail.com"), Map.of("email", "test2@mail.com"));
        when(userService.getFollowingForUser("500")).thenReturn(mockData);

        System.out.println("Mock service will return: " + mockData);

        ResponseEntity<?> res = userController.getCurrentUserFollowing();
        System.out.println("Result: " + res.getBody());

        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockData, res.getBody());

        // Unauthenticated
        SecurityContextHolder.clearContext();
        ResponseEntity<?> unauthRes = userController.getCurrentUserFollowing();
        System.out.println("Result (unauthenticated): " + unauthRes.getBody());

        assertEquals(401, unauthRes.getStatusCodeValue());
        System.out.println("following test completed\n");
    }

    @Test
    void followers_test() {
        System.out.println("Running followers test");

        SupabaseUser user = new SupabaseUser("600", "testf@mail.com", Map.of());
        mockAuth(user);

        List<Map<String, Object>> mockData = List.of(Map.of("email", "test1@mail.com"), Map.of("email", "test2@mail.com"));
        when(userService.getFollowersForUser("600")).thenReturn(mockData);

        System.out.println("Mock service will return: " + mockData);

        ResponseEntity<?> res = userController.getCurrentUserFollowers();
        System.out.println("Result: " + res.getBody());

        assertEquals(200, res.getStatusCodeValue());
        assertEquals(mockData, res.getBody());

        // Unauthenticated
        SecurityContextHolder.clearContext();
        ResponseEntity<?> unauthRes = userController.getCurrentUserFollowers();
        System.out.println("Result (unauthenticated): " + unauthRes.getBody());

        assertEquals(401, unauthRes.getStatusCodeValue());
        System.out.println("followers test completed\n");
    }
}
