package com.trustworthyreviews.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Get all products
     */
    public List<Map<String, Object>> getAllProducts() {
        /**
         * Circuit Breaker Testing: Failure Simulation
         * 
         * Checks a system property "circuit.breaker.simulate.product.failure" to determine
         * if a failure should be simulated. This property is set by CircuitBreakerDebugController
         * when failure simulation is enabled for ProductService.
         * 
         * When the property is "true", this method throws an exception to simulate a database
         * failure. The exception occurs inside the Hystrix command, so it's properly tracked
         * by the circuit breaker metrics.
         * 
         * The property is set before the Hystrix call and cleared after (in the debug controller),
         * ensuring it only affects the intended test request.
         * 
         * WARNING: This is for testing only. In production, this check could be removed
         * or gated behind a feature flag.
         */
        if (Boolean.getBoolean("circuit.breaker.simulate.product.failure")) {
            throw new RuntimeException("Simulated database failure for circuit breaker testing");
        }
        
        String sql = """
            SELECT 
                product_id,
                name,
                avg_rating,
                description,
                image,
                link,
                category
            FROM products
            ORDER BY name
        """;
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Get product by ID
     */
    public Map<String, Object> getProductById(int productId) {
        String sql = """
            SELECT 
                product_id,
                name,
                avg_rating,
                description,
                image,
                link,
                category
            FROM products
            WHERE product_id = ?
        """;

        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, productId);
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Search products by name or category
     */
    public List<Map<String, Object>> searchProducts(String query) {
        String sql = """
            SELECT 
                product_id,
                name,
                avg_rating,
                description,
                image,
                link,
                category
            FROM products
            WHERE name ILIKE ? OR category ILIKE ?
            ORDER BY name
        """;

        String pattern = "%" + query + "%";
        return jdbcTemplate.queryForList(sql, pattern, pattern);
    }

    public List<Map<String, Object>> getProductsFiltered(String category, String userId, boolean onlyFollowing) {
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT
                p.product_id,
                p.name,
                p.avg_rating,
                p.description,
                p.image,
                p.link,
                p.category
            FROM products p
        """);

        // If filtering by following, join with reviews and relations
        if (onlyFollowing && userId != null) {
            sql.append("""
                INNER JOIN product_reviews pr ON p.product_id = pr.product_id
                INNER JOIN relations r ON pr.uid = r.following
                WHERE r.uid = ?::uuid
            """);
        } else {
            sql.append(" WHERE 1=1 ");
        }

        // Add category filter if provided
        if (category != null && !category.isEmpty() && !"all".equalsIgnoreCase(category)) {
            if (onlyFollowing && userId != null) {
                sql.append(" AND p.category = ? ");
            } else {
                sql.append(" AND p.category = ? ");
            }
        }

        // Order by average rating descending, then by name
        sql.append(" ORDER BY p.avg_rating DESC NULLS LAST, p.name");

        // Build parameters list
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (onlyFollowing && userId != null) {
            params.add(userId);
        }
        if (category != null && !category.isEmpty() && !"all".equalsIgnoreCase(category)) {
            params.add(category);
        }

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * Get all distinct categories
     */
    public List<String> getAllCategories() {
        String sql = """
            SELECT DISTINCT category
            FROM products
            WHERE category IS NOT NULL
            ORDER BY category
        """;
        
        return jdbcTemplate.queryForList(sql, String.class);
    }
}
