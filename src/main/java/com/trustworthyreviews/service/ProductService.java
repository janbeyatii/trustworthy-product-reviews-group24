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
    void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
