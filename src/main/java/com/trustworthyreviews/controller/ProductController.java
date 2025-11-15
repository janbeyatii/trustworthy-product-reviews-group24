package com.trustworthyreviews.controller;

import com.trustworthyreviews.service.HystrixProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private HystrixProductService hystrixProductService;

    /**
     * Get all products
     */
    @GetMapping
    public List<Map<String, Object>> getAllProducts() {
        return hystrixProductService.getAllProducts();
    }

    /**
     * Get a single product by ID
     */
    @GetMapping("/{id}")
    public Map<String, Object> getProductById(@PathVariable("id") int productId) {
        return hystrixProductService.getProductById(productId);
    }

    /**
     * Search products by query (name or category)
     */
    @GetMapping("/search")
    public List<Map<String, Object>> searchProducts(@RequestParam("q") String query) {
        return hystrixProductService.searchProducts(query);
    }

}
