package com.trustworthyreviews.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustworthyreviews.config.SupabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final JdbcTemplate jdbcTemplate;
    private final SupabaseConfig.SupabaseProperties supabaseProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public UserService(JdbcTemplate jdbcTemplate,
                       SupabaseConfig.SupabaseProperties supabaseProperties,
                       ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.supabaseProperties = supabaseProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Search users by display name or email
     * This queries Supabase auth.users table through the raw database connection
     */
    public List<Map<String, Object>> searchUsers(String query) {
        try {
            /**
             * Circuit Breaker Testing: Failure Simulation
             * 
             * If the query starts with "__SIMULATE_FAILURE__", this method throws an exception
             * to simulate a database failure. This is used by the CircuitBreakerDebugController
             * to test circuit breaker behavior without requiring actual database failures.
             * 
             * The failure marker is added by the debug controller when failure simulation is enabled.
             * The exception occurs inside the Hystrix command, so it's properly tracked by the
             * circuit breaker metrics.
             * 
             * WARNING: This is for testing only. In production, this check could be removed
             * or gated behind a feature flag.
             */
            if (query != null && query.startsWith("__SIMULATE_FAILURE__")) {
                throw new RuntimeException("Simulated database failure for circuit breaker testing");
            }
            
            // Query auth.users table to find users matching the query
            String sql = """
                SELECT 
                    id,
                    email,
                    raw_user_meta_data,
                    created_at
                FROM auth.users
                WHERE 
                    email ILIKE ?
                ORDER BY email
                LIMIT 20
            """;
            
            String pattern = "%" + query + "%";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, pattern);
            
            enrichUserMetadata(users);
            
            return users;
        } catch (Exception e) {
            throw new RuntimeException("Error searching users: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by UUID
     */
    public Map<String, Object> getUserById(String userId) {
        try {
            log.info("Fetching user by ID: {}", userId);

            List<Map<String, Object>> results = queryUserById(userId);
            if (!results.isEmpty()) {
                log.info("Successfully fetched user: {}", results.get(0));
                return results.get(0);
            }

            log.warn("No user found with ID: {}", userId);
            return null;
        } catch (Exception e) {
            log.warn("Database lookup for user {} failed: {}. Attempting Supabase admin API fallback.", userId, e.getMessage());

            Optional<Map<String, Object>> fallbackUser = fetchUserFromSupabaseAdmin(userId);
            if (fallbackUser.isPresent()) {
                return fallbackUser.get();
            }

            log.error("Error fetching user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error fetching user: " + e.getMessage(), e);
        }
    }

    /**
     * Get the users that the provided user is following, including email metadata.
     */
    public List<Map<String, Object>> getFollowingForUser(String userId) {
        try {
            String sql = """
                SELECT 
                    u.id,
                    u.email,
                    u.raw_user_meta_data
                FROM public.relations r
                JOIN auth.users u ON u.id = r.following
                WHERE r.uid = ?
                ORDER BY u.email
            """;

            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, userId);
            enrichUserMetadata(users);
            return users;
        } catch (Exception e) {
            log.error("Error fetching following for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error fetching following: " + e.getMessage(), e);
        }
    }

    /**
     * Get the followers for the provided user, including email metadata.
     */
    public List<Map<String, Object>> getFollowersForUser(String userId) {
        try {
            String sql = """
                SELECT 
                    u.id,
                    u.email,
                    u.raw_user_meta_data
                FROM public.relations r
                JOIN auth.users u ON u.id = r.uid
                WHERE r.following = ?
                ORDER BY u.email
            """;

            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, userId);
            enrichUserMetadata(users);
            return users;
        } catch (Exception e) {
            log.error("Error fetching followers for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error fetching followers: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to query a user by ID from the database.
     */
    private List<Map<String, Object>> queryUserById(String userId) {
        String sql = """
                SELECT 
                    id,
                    email,
                    created_at,
                    raw_user_meta_data
                FROM auth.users
                WHERE id = ?
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
        enrichUserMetadata(results);
        results.forEach(row -> row.remove("raw_user_meta_data"));
        return results;
    }

    /**
     * Parse the raw_user_meta_data JSON field to extract a "display_name" key.
     */
    private void enrichUserMetadata(List<Map<String, Object>> users) {
        users.forEach(user -> {
            Object metadata = user.get("raw_user_meta_data");
            if (metadata == null) {
                return;
            }

            Map<String, Object> metaMap = null;
            if (metadata instanceof Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> castMap = (Map<String, Object>) rawMap;
                metaMap = castMap;
            } else {
                String json = metadata.toString();
                if (StringUtils.hasText(json) && json.trim().startsWith("{")) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
                        metaMap = parsed;
                    } catch (Exception ex) {
                        log.debug("Failed to parse user metadata for user {}: {}", user.get("id"), ex.getMessage());
                    }
                }
            }

            if (metaMap != null && metaMap.containsKey("display_name")) {
                user.put("display_name", metaMap.get("display_name"));
            }

            user.remove("raw_user_meta_data");
        });
    }

    /**
     * Fallback method to fetch user information from the Supabase Admin API if the local database lookup fails.
     */
    private Optional<Map<String, Object>> fetchUserFromSupabaseAdmin(String userId) {
        if (supabaseProperties == null
                || !StringUtils.hasText(supabaseProperties.getServiceRoleKey())
                || !StringUtils.hasText(supabaseProperties.getUrl())) {
            log.debug("Supabase service role key or URL not configured. Cannot query admin API.");
            return Optional.empty();
        }

        try {
            String baseUrl = supabaseProperties.getUrl().replaceAll("/+$", "");
            String endpoint = baseUrl + "/auth/v1/admin/users/" + userId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + supabaseProperties.getServiceRoleKey())
                    .header("apikey", supabaseProperties.getServiceRoleKey())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 404) {
                log.warn("Supabase admin API returned 404 for user {}", userId);
                return Optional.empty();
            }

            if (status < 200 || status >= 300) {
                log.error("Supabase admin API error (status {}): {}", status, response.body());
                return Optional.empty();
            }

            JsonNode node = objectMapper.readTree(response.body());
            Map<String, Object> user = new HashMap<>();
            user.put("id", node.path("id").asText(null));
            user.put("email", node.path("email").asText(null));

            JsonNode metaNode = node.path("user_metadata");
            if (!metaNode.isMissingNode() && !metaNode.isNull()) {
                Map<String, Object> metaMap = objectMapper.convertValue(metaNode, new TypeReference<Map<String, Object>>() {});
                if (metaMap.containsKey("display_name")) {
                    user.put("display_name", metaMap.get("display_name"));
                }
            }

            return Optional.of(user);
        } catch (Exception ex) {
            log.error("Failed to fetch user {} via Supabase admin API: {}", userId, ex.getMessage(), ex);
            return Optional.empty();
        }
    }
}

