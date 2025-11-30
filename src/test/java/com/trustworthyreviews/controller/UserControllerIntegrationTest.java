package com.trustworthyreviews.controller;

import com.trustworthyreviews.security.SupabaseUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------------- Helper ----------------
    private void loginAsRealUser(String id, String email, Map<String, Object> metadata) {
        SupabaseUser user = new SupabaseUser(id, email, metadata);
        // Use empty authorities list for simplicity
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ---------------- Tests ----------------

    @Test
    public void whoAmI_integration_unauthorized() throws Exception {
        SecurityContextHolder.clearContext(); // no user
        mockMvc.perform(get("/api/whoami").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // expect 401
    }

    @Test
    public void whoAmI_integration_realUser1() throws Exception {
        loginAsRealUser(
                "bf7e6e6b-70dc-4270-b609-1a367f1241bb",
                "test1@test.com",
                Map.of("name", "F")
        );

        mockMvc.perform(get("/api/whoami").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // expect 200
    }

    @Test
    public void whoAmI_integration_realUser2() throws Exception {
        loginAsRealUser(
                "490a2cba-3170-495d-9991-3e5855a7f00d",
                "testm@gmail.com",
                Map.of("display_name", "Test-M")
        );

        mockMvc.perform(get("/api/whoami").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void following_integration_unauthorized() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/users/me/following").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void following_integration_realUser1() throws Exception {
        loginAsRealUser(
                "bf7e6e6b-70dc-4270-b609-1a367f1241bb",
                "test1@test.com",
                Map.of("name", "F")
        );

        mockMvc.perform(get("/api/users/me/following").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void followers_integration_unauthorized() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/users/me/followers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void followers_integration_realUser2() throws Exception {
        loginAsRealUser(
                "490a2cba-3170-495d-9991-3e5855a7f00d",
                "testm@gmail.com",
                Map.of("display_name", "Test-M")
        );

        mockMvc.perform(get("/api/users/me/followers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void searchUsers_integration_unauthorized() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/users/search")
                        .param("query", "test")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void searchUsers_integration_realUser1() throws Exception {
        loginAsRealUser(
                "bf7e6e6b-70dc-4270-b609-1a367f1241bb",
                "test1@test.com",
                Map.of("name", "F")
        );

        mockMvc.perform(get("/api/users/search")
                        .param("query", "test")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getSimilarUsers_integration_unauthorized() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/users/me/similar")
                        .param("limit", "10")
                        .param("minSimilarity", "0.1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getSimilarUsers_integration_realUser1() throws Exception {
        loginAsRealUser(
                "bf7e6e6b-70dc-4270-b609-1a367f1241bb",
                "test1@test.com",
                Map.of("name", "F")
        );

        mockMvc.perform(get("/api/users/me/similar")
                        .param("limit", "10")
                        .param("minSimilarity", "0.1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getSimilarUsers_integration_realUser2() throws Exception {
        loginAsRealUser(
                "490a2cba-3170-495d-9991-3e5855a7f00d",
                "testm@gmail.com",
                Map.of("display_name", "Test-M")
        );

        mockMvc.perform(get("/api/users/me/similar")
                        .param("limit", "5")
                        .param("minSimilarity", "0.2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void calculateSimilarity_integration_unauthorized() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/users/similarity/some-user-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void calculateSimilarity_integration_realUser1() throws Exception {
        loginAsRealUser(
                "bf7e6e6b-70dc-4270-b609-1a367f1241bb",
                "test1@test.com",
                Map.of("name", "F")
        );

        mockMvc.perform(get("/api/users/similarity/another-user-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void calculateSimilarity_integration_realUser2() throws Exception {
        loginAsRealUser(
                "490a2cba-3170-495d-9991-3e5855a7f00d",
                "testm@gmail.com",
                Map.of("display_name", "Test-M")
        );

        mockMvc.perform(get("/api/users/similarity/another-user-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
