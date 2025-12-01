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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)  // disable Spring Security filters for integration tests
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------------- Helper ----------------
    private void login(String id, String email, String displayName) {
        SupabaseUser user = new SupabaseUser(
                id,
                email,
                Map.of("display_name", displayName)
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        java.util.Collections.emptyList()
                );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ---------------- Tests ----------------

    @Test
    public void whoAmI_unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/whoami")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void whoAmI_realUser1() throws Exception {
        login(
                "bf7e6e6b-70dc-4270-b609-1a367f1241bb",
                "test1@test.com",
                "F"
        );

        mockMvc.perform(get("/api/whoami")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void whoAmI_realUser2() throws Exception {
        login(
                "490a2cba-3170-495d-9991-3e5855a7f00d",
                "testm@gmail.com",
                "Test-M"
        );

        mockMvc.perform(get("/api/whoami")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void following_unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/users/me/following")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void following_realUser1() throws Exception {
        login(
                "bf7e6e6b-70dc-4270-b609-1a367f1241bb",
                "test1@test.com",
                "F"
        );

        mockMvc.perform(get("/api/users/me/following")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void followers_unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/users/me/followers")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void followers_realUser2() throws Exception {
        login(
                "490a2cba-3170-495d-9991-3e5855a7f00d",
                "testm@gmail.com",
                "Test-M"
        );

        mockMvc.perform(get("/api/users/me/followers")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void searchUsers_unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/users/search")
                        .param("query", "test")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void searchUsers_realUser1() throws Exception {
        login(
                "bf7e6e6b-70dc-4270-b609-1a367f1241bb",
                "test1@test.com",
                "F"
        );

        mockMvc.perform(get("/api/users/search")
                        .param("query", "test")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getSimilarUsers_unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/users/me/similar")
                        .param("limit", "10")
                        .param("minSimilarity", "0.1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getSimilarUsers_realUser1() throws Exception {
        login(
                "bf7e6e6b-70dc-4270-b609-1a367f1241bb",
                "test1@test.com",
                "F"
        );

        mockMvc.perform(get("/api/users/me/similar")
                        .param("limit", "10")
                        .param("minSimilarity", "0.1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getSimilarUsers_realUser2() throws Exception {
        login(
                "490a2cba-3170-495d-9991-3e5855a7f00d",
                "testm@gmail.com",
                "Test-M"
        );

        mockMvc.perform(get("/api/users/me/similar")
                        .param("limit", "5")
                        .param("minSimilarity", "0.2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void calculateSimilarity_unauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/users/similarity/some-user-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void calculateSimilarity_realUser1() throws Exception {
        login(
                "bf7e6e6b-70dc-4270-b609-1a367f1241bb",
                "test1@test.com",
                "F"
        );

        mockMvc.perform(get("/api/users/similarity/another-user-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void calculateSimilarity_realUser2() throws Exception {
        login(
                "490a2cba-3170-495d-9991-3e5855a7f00d",
                "testm@gmail.com",
                "Test-M"
        );

        mockMvc.perform(get("/api/users/similarity/another-user-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
