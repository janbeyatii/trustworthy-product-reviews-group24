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

    public List<Map<String, Object>> searchUsers(String query) {
        try {
            if (query != null && query.startsWith("__SIMULATE_FAILURE__")) {
                throw new RuntimeException("Simulated database failure for circuit breaker testing");
            }
            
            String sql = """
                SELECT 
                    id,
                    email,
                    raw_user_meta_data,
                    created_at
                FROM auth.users
                WHERE 
                    email ILIKE ?
                    OR
                    raw_user_meta_data::text ILIKE CONCAT('%display_name": "', ?)
                ORDER BY email
                LIMIT 20
            """;
            
            String email_pattern = "%" + query + "%";
            String display_name_pattern = query + "%";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, email_pattern, display_name_pattern);
            
            enrichUserMetadata(users);
            
            return users;
        } catch (Exception e) {
            throw new RuntimeException("Error searching users: " + e.getMessage(), e);
        }
    }

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

    public double calculateProductJaccardSimilarity(String userId1, String userId2) {
        try {
            String sql1 = "SELECT DISTINCT product_id FROM product_reviews WHERE uid = ?::uuid";
            List<Map<String, Object>> user1Products = jdbcTemplate.queryForList(sql1, userId1);
            
            List<Map<String, Object>> user2Products = jdbcTemplate.queryForList(sql1, userId2);
            
            java.util.Set<Object> set1 = user1Products.stream()
                    .map(row -> row.get("product_id"))
                    .collect(java.util.stream.Collectors.toSet());
            
            java.util.Set<Object> set2 = user2Products.stream()
                    .map(row -> row.get("product_id"))
                    .collect(java.util.stream.Collectors.toSet());
            
            return calculateJaccardIndex(set1, set2);
        } catch (Exception e) {
            log.error("Error calculating product Jaccard similarity between users {} and {}: {}", 
                    userId1, userId2, e.getMessage(), e);
            return 0.0;
        }
    }

    public double calculateFollowingJaccardSimilarity(String userId1, String userId2) {
        try {
            String sql = "SELECT DISTINCT following FROM relations WHERE uid = ?::uuid";
            List<Map<String, Object>> user1Following = jdbcTemplate.queryForList(sql, userId1);
            
            List<Map<String, Object>> user2Following = jdbcTemplate.queryForList(sql, userId2);
            
            java.util.Set<Object> set1 = user1Following.stream()
                    .map(row -> row.get("following"))
                    .collect(java.util.stream.Collectors.toSet());
            
            java.util.Set<Object> set2 = user2Following.stream()
                    .map(row -> row.get("following"))
                    .collect(java.util.stream.Collectors.toSet());
            
            return calculateJaccardIndex(set1, set2);
        } catch (Exception e) {
            log.error("Error calculating following Jaccard similarity between users {} and {}: {}", 
                    userId1, userId2, e.getMessage(), e);
            return 0.0;
        }
    }

    public double calculateCombinedJaccardSimilarity(String userId1, String userId2) {
        double productSimilarity = calculateProductJaccardSimilarity(userId1, userId2);
        double followingSimilarity = calculateFollowingJaccardSimilarity(userId1, userId2);
        
        return (productSimilarity + followingSimilarity) / 2.0;
    }

    public List<Map<String, Object>> findSimilarUsers(String userId, int limit, double minSimilarity) {
        try {
            String sql = """
                SELECT DISTINCT u.id, u.email, u.raw_user_meta_data
                FROM auth.users u
                WHERE u.id != ?::uuid
                AND (
                    EXISTS (SELECT 1 FROM product_reviews WHERE uid = u.id)
                    OR EXISTS (SELECT 1 FROM relations WHERE uid = u.id)
                )
                LIMIT 100
            """;
            
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, userId);
            enrichUserMetadata(users);
            
            List<Map<String, Object>> similarUsers = new java.util.ArrayList<>();
            for (Map<String, Object> user : users) {
                String otherUserId = user.get("id").toString();
                double similarity = calculateCombinedJaccardSimilarity(userId, otherUserId);
                
                if (similarity >= minSimilarity) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", user.get("id"));
                    result.put("email", user.get("email"));
                    result.put("display_name", user.get("display_name"));
                    result.put("similarity", Math.round(similarity * 1000.0) / 1000.0); // Round to 3 decimals
                    result.put("product_similarity", Math.round(calculateProductJaccardSimilarity(userId, otherUserId) * 1000.0) / 1000.0);
                    result.put("following_similarity", Math.round(calculateFollowingJaccardSimilarity(userId, otherUserId) * 1000.0) / 1000.0);
                    similarUsers.add(result);
                }
            }
            
            similarUsers.sort((a, b) -> {
                double simA = (double) a.get("similarity");
                double simB = (double) b.get("similarity");
                return Double.compare(simB, simA);
            });
            
            return similarUsers.stream()
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error finding similar users for {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error finding similar users: " + e.getMessage(), e);
        }
    }

    private double calculateJaccardIndex(java.util.Set<Object> set1, java.util.Set<Object> set2) {
        if (set1.isEmpty() && set2.isEmpty()) {
            return 0.0;
        }
        
        java.util.Set<Object> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);
        
        java.util.Set<Object> union = new java.util.HashSet<>(set1);
        union.addAll(set2);
        
        return (double) intersection.size() / union.size();
    }
}

