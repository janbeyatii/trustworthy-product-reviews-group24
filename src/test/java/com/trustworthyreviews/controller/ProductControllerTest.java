package com.trustworthyreviews.controller;

import com.trustworthyreviews.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        log("Mocks initialized");
    }

    // Logging helpers
    private void log(String msg) {
        System.out.println(">>> " + msg);
    }

    private void logStart(String name) {
        System.out.println("\n=== Running " + name + " ===");
    }

    private void logData(Object data) {
        System.out.println("Data: " + data);
    }

    @Test
    void getAllProducts() {
        logStart("getAllProducts test");

        List<Map<String, Object>> mockProducts = new ArrayList<>();

        Map<String, Object> p1 = new HashMap<>();
        p1.put("id", 1);
        p1.put("name", "Ryzen 7 5800x3d");
        p1.put("category", "cpu");

        Map<String, Object> p2 = new HashMap<>();
        p2.put("id", 2);
        p2.put("name", "Ryzen 7 9800x3d");
        p2.put("category", "cpu");

        mockProducts.add(p1);
        mockProducts.add(p2);

        logData(mockProducts);

        when(productService.getAllProducts()).thenReturn(mockProducts);
        log("getAllProducts");

        List<Map<String, Object>> result = productController.getAllProducts();
        logData(result);

        assertEquals(2, result.size());
        assertEquals("Ryzen 7 5800x3d", result.get(0).get("name"));
        assertEquals("cpu", result.get(0).get("category"));

        verify(productService, times(1)).getAllProducts();
    }

    @Test
    void getProductById() {
        logStart("getProductById test");

        // Mock product with ID 10
        Map<String, Object> mockProduct = new HashMap<>();
        mockProduct.put("id", 10);
        mockProduct.put("name", "Ryzen 7 5800x3d");
        mockProduct.put("category", "cpu");

        // Another product that should NOT be returned
        Map<String, Object> otherProduct = new HashMap<>();
        otherProduct.put("id", 11);
        otherProduct.put("name", "Ryzen 7 9800x3d");
        otherProduct.put("category", "cpu");

        logData(List.of(mockProduct, otherProduct));

        // productService.getProductById(10) call
        when(productService.getProductById(10)).thenReturn(mockProduct);
        log("getProductById(10)");

        Map<String, Object> result = productController.getProductById(10);
        logData(result);

        // Assertions: ensure only the product with ID 10 is returned
        assertEquals(10, result.get("id"));
        assertEquals("Ryzen 7 5800x3d", result.get("name"));
        assertEquals("cpu", result.get("category"));

        verify(productService, times(1)).getProductById(10);
    }

    @Test
    void searchProducts() {
        logStart("searchProducts test");

        // All products
        List<Map<String, Object>> allProducts = new ArrayList<>();

        Map<String, Object> cpu = new HashMap<>();
        cpu.put("id", 3);
        cpu.put("name", "Ryzen 7 5800x3d");
        cpu.put("category", "cpu");

        Map<String, Object> gpu = new HashMap<>();
        gpu.put("id", 4);
        gpu.put("name", "5080 Graphic Card");
        gpu.put("category", "gpu");

        allProducts.add(cpu);
        allProducts.add(gpu);

        logData(allProducts);

        // Mock productService.searchProducts to filter by name or category
        when(productService.searchProducts(anyString())).thenAnswer(invocation -> {
            String query = invocation.getArgument(0);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> p : allProducts) {
                if (p.get("name").equals(query) || p.get("category").equals(query)) {
                    result.add(p);
                }
            }
            return result;
        });

        // Search by NAME
        log("Searching by NAME: 'Ryzen 7 5800x3d'");
        List<Map<String, Object>> resultByName = productController.searchProducts("Ryzen 7 5800x3d");
        logData(resultByName);

        assertEquals(1, resultByName.size());
        assertEquals("Ryzen 7 5800x3d", resultByName.get(0).get("name"));
        assertEquals("cpu", resultByName.get(0).get("category"));

        // Search by CATEGORY
        log("Searching by CATEGORY: 'gpu'");
        List<Map<String, Object>> resultByCategory = productController.searchProducts("gpu");
        logData(resultByCategory);

        assertEquals(1, resultByCategory.size());
        assertEquals("5080 Graphic Card", resultByCategory.get(0).get("name"));
        assertEquals("gpu", resultByCategory.get(0).get("category"));

        // Verify that searchProducts was called correctly
        verify(productService, times(1)).searchProducts("Ryzen 7 5800x3d");
        verify(productService, times(1)).searchProducts("gpu");
    }

}
