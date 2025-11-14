package com.trustworthyreviews.service;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Hystrix-wrapped ProductService methods for circuit breaker protection
 */
@Service
public class HystrixProductService {

    private static final Logger log = LoggerFactory.getLogger(HystrixProductService.class);
    
    private final ProductService productService;

    public HystrixProductService(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Get all products with circuit breaker protection
     */
    public List<Map<String, Object>> getAllProducts() {
        return new GetAllProductsCommand(productService).execute();
    }

    /**
     * Get product by ID with circuit breaker protection
     */
    public Map<String, Object> getProductById(int productId) {
        return new GetProductByIdCommand(productId, productService).execute();
    }

    /**
     * Search products with circuit breaker protection
     */
    public List<Map<String, Object>> searchProducts(String query) {
        return new SearchProductsCommand(query, productService).execute();
    }

    // Hystrix Commands

    private static class GetAllProductsCommand extends HystrixCommand<List<Map<String, Object>>> {
        private final ProductService productService;

        protected GetAllProductsCommand(ProductService productService) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Database"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("GetAllProducts"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                            .withCircuitBreakerEnabled(true)
                            .withCircuitBreakerRequestVolumeThreshold(10)
                            .withCircuitBreakerErrorThresholdPercentage(50)
                            .withCircuitBreakerSleepWindowInMilliseconds(5000)
                            .withExecutionTimeoutInMilliseconds(3000)
                            .withFallbackEnabled(true)));
            this.productService = productService;
        }

        @Override
        protected List<Map<String, Object>> run() throws Exception {
            return productService.getAllProducts();
        }

        @Override
        protected List<Map<String, Object>> getFallback() {
            log.warn("GetAllProducts circuit breaker opened or timed out. Returning empty list.");
            return Collections.emptyList();
        }
    }

    private static class GetProductByIdCommand extends HystrixCommand<Map<String, Object>> {
        private final int productId;
        private final ProductService productService;

        protected GetProductByIdCommand(int productId, ProductService productService) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Database"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("GetProductById"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                            .withCircuitBreakerEnabled(true)
                            .withCircuitBreakerRequestVolumeThreshold(10)
                            .withCircuitBreakerErrorThresholdPercentage(50)
                            .withCircuitBreakerSleepWindowInMilliseconds(5000)
                            .withExecutionTimeoutInMilliseconds(3000)
                            .withFallbackEnabled(true)));
            this.productId = productId;
            this.productService = productService;
        }

        @Override
        protected Map<String, Object> run() throws Exception {
            return productService.getProductById(productId);
        }

        @Override
        protected Map<String, Object> getFallback() {
            log.warn("GetProductById circuit breaker opened or timed out for product {}. Returning null.", productId);
            return null;
        }
    }

    private static class SearchProductsCommand extends HystrixCommand<List<Map<String, Object>>> {
        private final String query;
        private final ProductService productService;

        protected SearchProductsCommand(String query, ProductService productService) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Database"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("SearchProducts"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                            .withCircuitBreakerEnabled(true)
                            .withCircuitBreakerRequestVolumeThreshold(10)
                            .withCircuitBreakerErrorThresholdPercentage(50)
                            .withCircuitBreakerSleepWindowInMilliseconds(5000)
                            .withExecutionTimeoutInMilliseconds(3000)
                            .withFallbackEnabled(true)));
            this.query = query;
            this.productService = productService;
        }

        @Override
        protected List<Map<String, Object>> run() throws Exception {
            return productService.searchProducts(query);
        }

        @Override
        protected List<Map<String, Object>> getFallback() {
            log.warn("SearchProducts circuit breaker opened or timed out for query '{}'. Returning empty list.", query);
            return Collections.emptyList();
        }
    }
}

