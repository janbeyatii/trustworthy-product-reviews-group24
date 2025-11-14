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
        // Check for simulation failure marker (for circuit breaker testing)
        // This is a hack - in real scenario, you'd check a flag from CircuitBreakerDebugController
        // For now, we'll use a thread-local or check a system property
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
}
