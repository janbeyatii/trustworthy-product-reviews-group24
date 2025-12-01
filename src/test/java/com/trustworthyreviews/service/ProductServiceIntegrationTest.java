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
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Create table if it does not exist
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS PRODUCTS (
                PRODUCT_ID INT PRIMARY KEY,
                NAME VARCHAR(255),
                AVG_RATING DOUBLE,
                DESCRIPTION VARCHAR(255),
                IMAGE VARCHAR(255),
                LINK VARCHAR(255),
                CATEGORY VARCHAR(255)
            )
        """);

        // Clear table before each test
        jdbcTemplate.update("DELETE FROM PRODUCTS");

        // Seed database with one product
        jdbcTemplate.update("""
            INSERT INTO PRODUCTS (PRODUCT_ID, NAME, AVG_RATING, DESCRIPTION, IMAGE, LINK, CATEGORY)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, 1, "ASUS Prime Radeon RX 9070 XT Graphics Card", 5.0, "gpu", "", "", "Graphics Card");
    }

    @Test
    void getAllProducts_returnsProducts() {
        List<Map<String, Object>> products = productService.getAllProducts();
        assertEquals(1, products.size());
        assertEquals("ASUS Prime Radeon RX 9070 XT Graphics Card", products.get(0).get("name"));
    }

    @Test
    void getProductById_existingProduct_returnsProduct() {
        Map<String, Object> product = productService.getProductById(1);
        assertNotNull(product);
        assertEquals(1, product.get("product_id"));
        assertEquals("ASUS Prime Radeon RX 9070 XT Graphics Card", product.get("name"));
    }

    @Test
    void getProductById_nonExistingProduct_returnsNull() {
        Map<String, Object> product = productService.getProductById(999);
        assertNull(product);
    }

    @Test
    void searchProducts_findsMatchingProduct() {
        List<Map<String, Object>> products = productService.searchProducts("ASUS");
        assertEquals(1, products.size());
        assertEquals("ASUS Prime Radeon RX 9070 XT Graphics Card", products.get(0).get("name"));
    }
}
