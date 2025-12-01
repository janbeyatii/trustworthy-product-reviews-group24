package com.trustworthyreviews.controller;

import com.trustworthyreviews.TestSecurityConfig;
import com.trustworthyreviews.service.ProductService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.profiles.active=test"
)@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
public class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService; 

    @Test
    public void getAllProducts_returnsOkAndNonEmpty() throws Exception {
        when(productService.getAllProducts())
                .thenReturn(List.of(Map.of("product_id", 1, "name", "ASUS Prime Radeon RX 9070 XT Graphics Card")));

        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].product_id").exists())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    public void getExistingProductById_returnsOk() throws Exception {
        when(productService.getProductById(1))
                .thenReturn(Map.of("product_id", 1, "name", "ASUS Prime Radeon RX 9070 XT Graphics Card"));

        mockMvc.perform(get("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value(1))
                .andExpect(jsonPath("$.name").value("ASUS Prime Radeon RX 9070 XT Graphics Card"));
    }

    @Test
    public void getNonExistingProductById_returnsEmpty() throws Exception {
        when(productService.getProductById(9999)).thenReturn(null);

        mockMvc.perform(get("/api/products/9999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    public void searchProducts_returnsOkAndArray() throws Exception {
        when(productService.searchProducts("ASUS"))
                .thenReturn(List.of(Map.of("product_id", 1, "name", "ASUS Prime Radeon RX 9070 XT Graphics Card")));

        mockMvc.perform(get("/api/products/search")
                        .param("q", "ASUS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void searchProducts_noMatch_returnsEmptyArray() throws Exception {
        when(productService.searchProducts("NonExistentProduct")).thenReturn(List.of());

        mockMvc.perform(get("/api/products/search")
                        .param("q", "NonExistentProduct")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
