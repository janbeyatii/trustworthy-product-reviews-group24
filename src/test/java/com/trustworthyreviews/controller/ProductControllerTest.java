package com.trustworthyreviews.controller;

import com.trustworthyreviews.security.SupabaseJwtFilter;
import com.trustworthyreviews.security.SupabaseJwtService;
import com.trustworthyreviews.service.HystrixProductService;
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

    @MockBean
    private SupabaseJwtFilter supabaseJwtFilter;

    @Test
    void getAllProducts_returnsOkAndJson() throws Exception {
        when(hystrixProductService.getAllProducts())
                .thenReturn(List.of(Map.of("product_id", 1, "name", "X")));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("X"));

        verify(hystrixProductService).getAllProducts();
    }

    @Test
    void getProductById_returnsOne() throws Exception {
        when(hystrixProductService.getProductById(1))
                .thenReturn(Map.of("product_id", 1, "name", "X"));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product_id").value(1));
    }

    @Test
    void searchProducts_forwardsQuery() throws Exception {
        when(hystrixProductService.searchProducts("ASUS Prime Radeon RX 9070 XT Graphics Card"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/products/search").param("q", "ASUS Prime Radeon RX 9070 XT Graphics Card"))
                .andExpect(status().isOk());

        verify(hystrixProductService).searchProducts("ASUS Prime Radeon RX 9070 XT Graphics Card");
    }
}
