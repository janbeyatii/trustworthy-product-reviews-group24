package com.trustworthyreviews.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getProductByIdIntegration_returnsOkForExistingAndEmptyForNonExisting() throws Exception {
        // Test existing product (ID 1)
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value(1))
                .andExpect(jsonPath("$.name").value("ASUS Prime Radeon RX 9070 XT Graphics Card"));


        // Test non-existing product (ID 9999)
        mockMvc.perform(get("/api/products/9999"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}
