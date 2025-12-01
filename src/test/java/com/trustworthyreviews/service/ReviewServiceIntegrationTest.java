/* 
package com.trustworthyreviews.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@AutoConfigureTestDatabase
class ReviewServiceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS products (
                product_id INT PRIMARY KEY,
                name VARCHAR(255),
                avg_rating DOUBLE
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(36) PRIMARY KEY,
                email VARCHAR(255),
                display_name VARCHAR(255)
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS product_reviews (
                review_id INT AUTO_INCREMENT PRIMARY KEY,
                product_id INT,
                review_rating INT,
                review_desc VARCHAR(255),
                uid VARCHAR(36),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);

        jdbcTemplate.update("DELETE FROM product_reviews");
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update("INSERT INTO products (product_id, name, avg_rating) VALUES (?, ?, ?)",
                1, "Test GPU", 0.0);
        jdbcTemplate.update("INSERT INTO users (id, email, display_name) VALUES (?, ?, ?)",
                "00000000-0000-0000-0000-000000000001", "alice@example.com", "Alice");
        jdbcTemplate.update("INSERT INTO users (id, email, display_name) VALUES (?, ?, ?)",
                "00000000-0000-0000-0000-000000000002", "bob@example.com", "Bob");
    }

    @Test
    void addReview_createsReviewSuccessfully() {
        Map<String, Object> result = reviewService.addReview(
                1, "00000000-0000-0000-0000-000000000001", 5, "Great product!"
        );

        assertNotNull(result.get("review_id"));
        assertEquals("Review added successfully", result.get("message"));

        List<Map<String, Object>> reviews = jdbcTemplate.queryForList(
                "SELECT * FROM product_reviews WHERE product_id = ?", 1
        );
        assertEquals(1, reviews.size());
        assertEquals(5, reviews.get(0).get("review_rating"));
    }

    @Test
    void addReview_duplicateReview_throwsException() {
        reviewService.addReview(1, "00000000-0000-0000-0000-000000000001", 5, "Great product!");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                reviewService.addReview(1, "00000000-0000-0000-0000-000000000001", 4, "Updated review")
        );

        assertEquals("You have already reviewed this product", ex.getMessage());
    }

    @Test
    void getReviewsForProduct_returnsReviewsWithUserData() {
        reviewService.addReview(1, "00000000-0000-0000-0000-000000000001", 5, "Amazing!");
        reviewService.addReview(1, "00000000-0000-0000-0000-000000000002", 4, "Good");

        List<Map<String, Object>> reviews = jdbcTemplate.queryForList(
                "SELECT r.review_id, r.product_id, r.review_rating, r.review_desc, r.uid, u.email, u.display_name " +
                        "FROM product_reviews r " +
                        "JOIN users u ON r.uid = u.id " +
                        "WHERE r.product_id = ?", 1
        );

        assertEquals(2, reviews.size());
        for (Map<String, Object> review : reviews) {
            assertNotNull(review.get("email"));
            assertNotNull(review.get("display_name"));
        }
    }

    @Test
    void getReviewSummary_returnsCorrectStats() {
        reviewService.addReview(1, "00000000-0000-0000-0000-000000000001", 5, "Amazing!");
        reviewService.addReview(1, "00000000-0000-0000-0000-000000000002", 3, "Okay");

        Map<String, Object> summary = reviewService.getReviewSummary(1);

        assertEquals(2L, summary.get("total_reviews"));
        assertEquals(4.0, ((Number) summary.get("avg_rating")).doubleValue());

        Map<Integer, Integer> distribution = (Map<Integer, Integer>) summary.get("distribution");
        assertEquals(1, distribution.get(5).intValue());
        assertEquals(1, distribution.get(3).intValue());
        assertEquals(0, distribution.get(1).intValue());
    }
}
*/