package com.trustworthyreviews.controller;

import com.trustworthyreviews.security.SupabaseJwtService;
import com.trustworthyreviews.service.HystrixProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HystrixProductService hystrixProductService;

    @MockBean
    private SupabaseJwtService supabaseJwtService;

    private List<Map<String, Object>> mockProducts;

    @BeforeEach
    void setUp() {
        mockProducts = List.of(
                Map.of("id", 1, "name", "Ryzen 7 5800x3d", "category", "cpu"),
                Map.of("id", 2, "name", "Intel i9 13900k", "category", "cpu"),
                Map.of("id", 3, "name", "RTX 4090", "category", "gpu")
        );
        log("Mock products initialized");
    }

    private void log(String msg) {
        System.out.println(msg);
    }

    private void logStart(String name) {
        System.out.println("\nRunning " + name + " test");
    }

    private void logData(Object data) {
        System.out.println("Mock service will return: " + data);
    }

    @Test
    void getAllProducts() throws Exception {
        logStart("getAllProducts");
        when(hystrixProductService.getAllProducts()).thenReturn(mockProducts);
        logData(mockProducts);

        var result = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn();

        log("Result: " + result.getResponse().getContentAsString());
        log("getAllProducts test completed");
        verify(hystrixProductService, times(1)).getAllProducts();
    }

    @Test
    void getProductById() throws Exception {
        Map<String, Object> product = Map.of("id", 1, "name", "Ryzen 7 5800x3d", "category", "cpu");

        logStart("getProductById");
        when(hystrixProductService.getProductById(1)).thenReturn(product);
        logData(product);

        var result = mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andReturn();

        log("Result: " + result.getResponse().getContentAsString());
        log("getProductById test completed");
        verify(hystrixProductService, times(1)).getProductById(1);
    }

    @Test
    void searchProductsByCategory() throws Exception {
        List<Map<String, Object>> searchResult = List.of(
                Map.of("id", 1, "name", "Ryzen 7 5800x3d", "category", "cpu"),
                Map.of("id", 2, "name", "Intel i9 13900k", "category", "cpu")
        );

        logStart("searchProductsByCategory");
        when(hystrixProductService.searchProducts("cpu")).thenReturn(searchResult);
        logData(searchResult);

        var result = mockMvc.perform(get("/api/products/search").param("q", "cpu"))
                .andExpect(status().isOk())
                .andReturn();

        log("Result: " + result.getResponse().getContentAsString());
        log("searchProductsByCategory test completed");
        verify(hystrixProductService, times(1)).searchProducts("cpu");
    }

    @Test
    void searchProductsByName() throws Exception {
        List<Map<String, Object>> searchResult = List.of(
                Map.of("id", 3, "name", "RTX 4090", "category", "gpu")
        );

        logStart("searchProductsByName");
        when(hystrixProductService.searchProducts("RTX 4090")).thenReturn(searchResult);
        logData(searchResult);

        var result = mockMvc.perform(get("/api/products/search").param("q", "RTX 4090"))
                .andExpect(status().isOk())
                .andReturn();

        log("Result: " + result.getResponse().getContentAsString());
        log("searchProductsByName test completed");
        verify(hystrixProductService, times(1)).searchProducts("RTX 4090");
    }
}
