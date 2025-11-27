package com.trustworthyreviews.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    @Transactional
    public Map<String, Object> addReview(int productId, String userId, int rating, String reviewText) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        try {
            String checkSql = "SELECT COUNT(*) FROM product_reviews WHERE product_id = ? AND uid = ?::uuid";
            Integer existingCount = jdbcTemplate.queryForObject(checkSql, Integer.class, productId, userId);
            
            if (existingCount != null && existingCount > 0) {
                throw new IllegalStateException("You have already reviewed this product");
            }

            String insertSql = """
                INSERT INTO product_reviews (product_id, review_rating, review_desc, uid)
                VALUES (?, ?, ?, ?::uuid)
            """;
            
            jdbcTemplate.update(insertSql, productId, rating, reviewText, userId);

            String getIdSql = """
                SELECT review_id FROM product_reviews 
                WHERE product_id = ? AND uid = ?::uuid
                ORDER BY created_at DESC
                LIMIT 1
            """;
            
            Integer reviewId = jdbcTemplate.queryForObject(getIdSql, Integer.class, productId, userId);

            updateProductAverageRating(productId);

            Map<String, Object> result = new HashMap<>();
            result.put("review_id", reviewId);
            result.put("message", "Review added successfully");
            
            log.info("User {} added review {} for product {}", userId, reviewId, productId);
            return result;

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error adding review for product {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("Error adding review: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getReviewsForProduct(int productId) {
        return getReviewsForProduct(productId, null);
    }

    public List<Map<String, Object>> getReviewsForProduct(int productId, String currentUserId) {
        try {
            String sql = """
                SELECT 
                    r.review_id,
                    r.product_id,
                    r.review_rating,
                    r.review_desc,
                    r.uid::text as uid,
                    r.created_at
                FROM product_reviews r
                WHERE r.product_id = ?
                ORDER BY r.created_at DESC
            """;
            
            List<Map<String, Object>> reviews = jdbcTemplate.queryForList(sql, productId);
            
            // Enrich with user data manually
            for (Map<String, Object> review : reviews) {
                String uid = (String) review.get("uid");
                if (uid != null) {
                    try {
                        Map<String, Object> user = userService.getUserById(uid);
                        if (user != null) {
                            review.put("email", user.get("email"));
                            review.put("display_name", user.get("display_name"));
                        }
                    } catch (Exception e) {
                        // ignore user fetch errors
                    }
                }
            }
            
            // Calculate degree of separation if current user is provided
            if (currentUserId != null) {
                for (Map<String, Object> review : reviews) {
                    String reviewerId = (String) review.get("uid");
                    if (reviewerId != null && !reviewerId.equals(currentUserId)) {
                        Integer degree = userService.getDegreeOfSeparation(currentUserId, reviewerId);
                        review.put("degree_of_separation", degree);
                    }
                }
            }
            
            return reviews;
        } catch (Exception e) {
            log.error("Error fetching reviews for product {}: {}", productId, e.getMessage(), e);
            throw new RuntimeException("Error fetching reviews: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getReviewSummary(int productId) {
        try {
            String distributionSql = """
                SELECT 
                    review_rating,
                    COUNT(*) as count
                FROM product_reviews
                WHERE product_id = ?
                GROUP BY review_rating
                ORDER BY review_rating DESC
            """;
            
            List<Map<String, Object>> distribution = jdbcTemplate.queryForList(distributionSql, productId);
            
            String statsSql = """
                SELECT 
                    COUNT(*) as total_reviews,
                    AVG(review_rating) as avg_rating
                FROM product_reviews
                WHERE product_id = ?
            """;
            
            Map<String, Object> stats = jdbcTemplate.queryForMap(statsSql, productId);
            
            Map<Integer, Integer> ratingDistribution = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                ratingDistribution.put(i, 0);
            }
            
            for (Map<String, Object> row : distribution) {
                Integer rating = ((Number) row.get("review_rating")).intValue();
                Integer count = ((Number) row.get("count")).intValue();
                ratingDistribution.put(rating, count);
            }
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("total_reviews", stats.get("total_reviews"));
            summary.put("avg_rating", stats.get("avg_rating"));
            summary.put("distribution", ratingDistribution);
            
            return summary;
            
        } catch (Exception e) {
            log.error("Error fetching review summary for product {}: {}", productId, e.getMessage(), e);
            
            Map<String, Object> emptySummary = new HashMap<>();
            emptySummary.put("total_reviews", 0);
            emptySummary.put("avg_rating", null);
            Map<Integer, Integer> emptyDistribution = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                emptyDistribution.put(i, 0);
            }
            emptySummary.put("distribution", emptyDistribution);
            return emptySummary;
        }
    }

    private void updateProductAverageRating(int productId) {
        String updateSql = """
            UPDATE products
            SET avg_rating = (
                SELECT AVG(review_rating)
                FROM product_reviews
                WHERE product_id = ?
            )
            WHERE product_id = ?
        """;
        
        jdbcTemplate.update(updateSql, productId, productId);
    }

    private void enrichReviewMetadata(List<Map<String, Object>> reviews) {
        reviews.forEach(review -> {
            Object metadata = review.get("raw_user_meta_data");
            
            if (metadata != null) {
                try {
                    if (metadata instanceof Map<?, ?>) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> metaMap = (Map<String, Object>) metadata;
                        if (metaMap.containsKey("display_name")) {
                            review.put("display_name", metaMap.get("display_name"));
                        }
                    }
                } catch (Exception ex) {
                    log.debug("Failed to parse metadata for review {}: {}", review.get("review_id"), ex.getMessage());
                }
            }
            
            review.remove("raw_user_meta_data");
        });
    }
}

