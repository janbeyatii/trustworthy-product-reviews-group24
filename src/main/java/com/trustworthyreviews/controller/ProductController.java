package com.trustworthyreviews.controller;

import com.trustworthyreviews.security.SupabaseUser;
import com.trustworthyreviews.service.HystrixProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private HystrixProductService hystrixProductService;

    @GetMapping
    public List<Map<String, Object>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "false") boolean onlyFollowing) {
        
        String userId = null;
        if (onlyFollowing) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof SupabaseUser user) {
                userId = user.getId();
            }
        }
        
        if ((category == null || category.isEmpty() || "all".equalsIgnoreCase(category)) && !onlyFollowing) {
            return hystrixProductService.getAllProducts();
        }
        
        return hystrixProductService.getProductsFiltered(category, userId, onlyFollowing);
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

    /**
     * Get all distinct categories
     */
    @GetMapping("/categories")
    public List<String> getAllCategories() {
        return hystrixProductService.getAllCategories();
    }

}
