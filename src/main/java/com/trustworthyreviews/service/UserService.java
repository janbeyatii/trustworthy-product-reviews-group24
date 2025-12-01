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
            
            String sql = "SELECT * FROM public.search_users_secure(?)";
            
            String searchPattern = "%" + query + "%";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, searchPattern);
            
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
                FROM public.relations r,
                LATERAL public.get_user_details(r.following) u
                WHERE r.uid = ?::uuid
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
                FROM public.relations r,
                LATERAL public.get_user_details(r.uid) u
                WHERE r.following = ?::uuid
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
        String sql = "SELECT * FROM public.get_user_details(?::uuid)";

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

    public double calculateRatingJaccardSimilarity(String userId1, String userId2) {
        try {
            String sql = "SELECT product_id, review_rating FROM product_reviews WHERE uid = ?::uuid AND review_rating IS NOT NULL ORDER BY created_at DESC";
            List<Map<String, Object>> user1Reviews = jdbcTemplate.queryForList(sql, userId1);
            
            List<Map<String, Object>> user2Reviews = jdbcTemplate.queryForList(sql, userId2);
            
            Map<Object, Number> user1Ratings = user1Reviews.stream()
                    .filter(row -> row.get("review_rating") != null)
                    .collect(java.util.stream.Collectors.toMap(
                            row -> row.get("product_id"),
                            row -> (Number) row.get("review_rating"),
                            (existing, replacement) -> existing
                    ));
            
            Map<Object, Number> user2Ratings = user2Reviews.stream()
                    .filter(row -> row.get("review_rating") != null)
                    .collect(java.util.stream.Collectors.toMap(
                            row -> row.get("product_id"),
                            row -> (Number) row.get("review_rating"),
                            (existing, replacement) -> existing
                    ));
            
            java.util.Set<Object> commonProducts = new java.util.HashSet<>(user1Ratings.keySet());
            commonProducts.retainAll(user2Ratings.keySet());
            
            if (commonProducts.isEmpty()) {
                log.debug("No common products with ratings for users {} and {}", userId1, userId2);
                return 0.0;
            }
            
            long similarRatings = commonProducts.stream()
                    .filter(productId -> {
                        double rating1 = user1Ratings.get(productId).doubleValue();
                        double rating2 = user2Ratings.get(productId).doubleValue();
                        double diff = Math.abs(rating1 - rating2);
                        log.debug("Product {}: User1 rating={}, User2 rating={}, diff={}, similar={}", 
                                productId, rating1, rating2, diff, diff <= 1.0);
                        return diff <= 1.0;
                    })
                    .count();
            
            double similarity = (double) similarRatings / commonProducts.size();
            log.debug("Rating similarity for users {} and {}: {}/{} common products = {}", 
                    userId1, userId2, similarRatings, commonProducts.size(), similarity);
            
            return similarity;
        } catch (Exception e) {
            log.error("Error calculating rating Jaccard similarity between users {} and {}: {}", 
                    userId1, userId2, e.getMessage(), e);
            return 0.0;
        }
    }

    public double calculateCombinedJaccardSimilarity(String userId1, String userId2) {
        return calculateCombinedJaccardSimilarity(userId1, userId2, false);
    }

    public double calculateCombinedJaccardSimilarity(String userId1, String userId2, boolean forceRecalculate) {
        if (!forceRecalculate) {
            Map<String, Object> cached = getCachedSimilarity(userId1, userId2);
            if (cached != null) {
                log.debug("Using cached similarity for users {} and {}", userId1, userId2);
                return ((Number) cached.get("similarity_score")).doubleValue();
            }
        }
        
        double productSimilarity = calculateProductJaccardSimilarity(userId1, userId2);
        double ratingSimilarity = calculateRatingJaccardSimilarity(userId1, userId2);
        double combinedSimilarity = (productSimilarity + ratingSimilarity) / 2.0;
        
        cacheSimilarity(userId1, userId2, combinedSimilarity, productSimilarity, ratingSimilarity);
        
        return combinedSimilarity;
    }
    
    private Map<String, Object> getCachedSimilarity(String userId1, String userId2) {
        try {
            String[] ordered = orderUserIds(userId1, userId2);
            String sql = """
                SELECT similarity_score, product_similarity, rating_similarity, last_calculated
                FROM user_similarity_cache
                WHERE uuid1 = ?::uuid AND uuid2 = ?::uuid
            """;
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, ordered[0], ordered[1]);
            if (!results.isEmpty()) {
                return results.get(0);
            }
        } catch (Exception e) {
            log.debug("Cache lookup failed for users {} and {}: {}", userId1, userId2, e.getMessage());
        }
        return null;
    }
    
    private void cacheSimilarity(String userId1, String userId2, double similarity, 
                                  double productSim, double ratingSim) {
        try {
            String[] ordered = orderUserIds(userId1, userId2);
            String sql = """
                INSERT INTO user_similarity_cache 
                    (uuid1, uuid2, similarity_score, product_similarity, rating_similarity, last_calculated)
                VALUES (?::uuid, ?::uuid, ?, ?, ?, NOW())
                ON CONFLICT (uuid1, uuid2) 
                DO UPDATE SET 
                    similarity_score = EXCLUDED.similarity_score,
                    product_similarity = EXCLUDED.product_similarity,
                    rating_similarity = EXCLUDED.rating_similarity,
                    last_calculated = NOW()
            """;
            
            jdbcTemplate.update(sql, ordered[0], ordered[1], similarity, productSim, ratingSim);
            log.debug("Cached similarity for users {} and {}: {}", userId1, userId2, similarity);
        } catch (Exception e) {
            log.warn("Failed to cache similarity for users {} and {}: {}", userId1, userId2, e.getMessage());
        }
    }
    
    private String[] orderUserIds(String userId1, String userId2) {
        // Ensure user_id_1 < user_id_2 for consistent storage
        if (userId1.compareTo(userId2) < 0) {
            return new String[]{userId1, userId2};
        } else {
            return new String[]{userId2, userId1};
        }
    }
    
    public Map<String, Double> getSimilarityWithComponents(String userId1, String userId2) {
        Map<String, Object> cached = getCachedSimilarity(userId1, userId2);
        
        double combinedSim;
        double productSim;
        double ratingSim;
        
        if (cached != null) {
            combinedSim = ((Number) cached.get("similarity_score")).doubleValue();
            productSim = ((Number) cached.get("product_similarity")).doubleValue();
            ratingSim = ((Number) cached.get("rating_similarity")).doubleValue();
        } else {
            productSim = calculateProductJaccardSimilarity(userId1, userId2);
            ratingSim = calculateRatingJaccardSimilarity(userId1, userId2);
            combinedSim = (productSim + ratingSim) / 2.0;
            cacheSimilarity(userId1, userId2, combinedSim, productSim, ratingSim);
        }
        
        Map<String, Double> result = new HashMap<>();
        result.put("similarity", combinedSim);
        result.put("product_similarity", productSim);
        result.put("rating_similarity", ratingSim);
        return result;
    }

    public List<Map<String, Object>> findSimilarUsers(String userId, int limit, double minSimilarity) {
        try {
            String sql = "SELECT * FROM public.get_active_users(?::uuid)";
            
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, userId);
            enrichUserMetadata(users);
            
            List<Map<String, Object>> similarUsers = new java.util.ArrayList<>();
            for (Map<String, Object> user : users) {
                String otherUserId = user.get("id").toString();
                
                Map<String, Object> cached = getCachedSimilarity(userId, otherUserId);
                double similarity;
                double productSim;
                double ratingSim;
                
                if (cached != null) {
                    similarity = ((Number) cached.get("similarity_score")).doubleValue();
                    productSim = ((Number) cached.get("product_similarity")).doubleValue();
                    ratingSim = ((Number) cached.get("rating_similarity")).doubleValue();
                } else {
                    productSim = calculateProductJaccardSimilarity(userId, otherUserId);
                    ratingSim = calculateRatingJaccardSimilarity(userId, otherUserId);
                    similarity = (productSim + ratingSim) / 2.0;
                    cacheSimilarity(userId, otherUserId, similarity, productSim, ratingSim);
                }
                
                if (similarity >= minSimilarity) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", user.get("id"));
                    result.put("email", user.get("email"));
                    result.put("display_name", user.get("display_name"));
                    result.put("similarity", Math.round(similarity * 1000.0) / 1000.0);
                    result.put("product_similarity", Math.round(productSim * 1000.0) / 1000.0);
                    result.put("rating_similarity", Math.round(ratingSim * 1000.0) / 1000.0);
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

    /**
     * Calculate the degree of separation between two users.
     * 
     * @param fromUserId Starting user ID
     * @param toUserId Target user ID
     * @return Degree of separation (1 = direct follow, 2 = friend of friend, etc.)
     *         Returns null if users are not connected within 6 degrees
     */
    public Integer getDegreeOfSeparation(String fromUserId, String toUserId) {
        if (fromUserId.equals(toUserId)) {
            return 0;  // Same user
        }

        try {
            String sql = """
                WITH RECURSIVE follow_paths AS (
                    -- Base case: Direct follows (degree 1)
                    SELECT 
                        following as user_id,
                        1 as degree,
                        ARRAY[uid, following]::uuid[] as path
                    FROM public.relations
                    WHERE uid = ?::uuid
                    
                    UNION ALL
                    
                    -- Recursive case: Follows of follows
                    SELECT 
                        r.following as user_id,
                        fp.degree + 1 as degree,
                        fp.path || r.following
                    FROM public.relations r
                    INNER JOIN follow_paths fp ON r.uid = fp.user_id
                    WHERE fp.degree < 6
                        AND r.following != ALL(fp.path)  -- Prevent cycles
                        AND NOT EXISTS (
                            SELECT 1 
                            FROM unnest(fp.path) as p(user_id) 
                            WHERE p.user_id = r.following
                        )
                )
                SELECT MIN(degree) as min_degree
                FROM follow_paths
                WHERE user_id = ?::uuid
            """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                    sql, fromUserId, toUserId);

            if (results.isEmpty() || results.get(0).get("min_degree") == null) {
                return null;  // Not connected within limit
            }

            return ((Number) results.get(0).get("min_degree")).intValue();

        } catch (Exception e) {
            log.error("Error calculating degree of separation from {} to {}: {}", 
                    fromUserId, toUserId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get extended user profile with metrics (similarity, degree of separation) relative to a viewer.
     */
    public Map<String, Object> getUserProfileWithMetrics(String targetUserId, String viewerUserId) {
        Map<String, Object> userProfile = getUserById(targetUserId);
        if (userProfile == null) {
            return null;
        }
        
        // Add metrics if a viewer is provided and it's not the same user
        if (viewerUserId != null && !viewerUserId.equals(targetUserId)) {
            // Calculate similarity
            double similarity = calculateCombinedJaccardSimilarity(viewerUserId, targetUserId);
            userProfile.put("similarity", Math.round(similarity * 1000.0) / 1000.0);
            
            // Calculate degree of separation
            Integer degree = getDegreeOfSeparation(viewerUserId, targetUserId);
            userProfile.put("degree_of_separation", degree);
        }
        
        return userProfile;
    }

    /**
     * Get the most followed users.
     * 
     * @param limit Maximum number of users to return
     * @return List of users ordered by follower count (descending)
     */
    public List<Map<String, Object>> getMostFollowedUsers(int limit) {
        try {
            String sql = """
                SELECT 
                    u.id,
                    u.email,
                    u.raw_user_meta_data,
                    COUNT(r.uid) as follower_count
                FROM auth.users u
                LEFT JOIN public.relations r ON r.following = u.id::uuid
                GROUP BY u.id, u.email, u.raw_user_meta_data
                HAVING COUNT(r.uid) > 0
                ORDER BY follower_count DESC
                LIMIT ?
            """;
            
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, limit);
            enrichUserMetadata(users);
            
            return users;
        } catch (Exception e) {
            log.error("Error fetching most followed users: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching most followed users: " + e.getMessage(), e);
        }
    }
}

