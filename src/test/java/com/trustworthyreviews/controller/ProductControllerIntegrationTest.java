package com.trustworthyreviews.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getAllProducts_returnsOkAndNonEmpty() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].product_id").exists())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    public void getExistingProductById_returnsOk() throws Exception {
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value(1))
                .andExpect(jsonPath("$.name").value("ASUS Prime Radeon RX 9070 XT Graphics Card"));
    }

    @Test
    public void getNonExistingProductById_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/products/9999"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    public void searchProducts_returnsOkAndArray() throws Exception {
        mockMvc.perform(get("/api/products/search").param("q", "ASUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void searchProducts_noMatch_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/products/search").param("q", "NonExistentProduct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
