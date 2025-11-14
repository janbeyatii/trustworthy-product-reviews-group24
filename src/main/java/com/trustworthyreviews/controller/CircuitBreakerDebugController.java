package com.trustworthyreviews.controller;

import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixCommandKey;
import com.trustworthyreviews.service.HystrixProductService;
import com.trustworthyreviews.service.HystrixUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Debug controller for testing and visualizing circuit breaker behavior
 * 
 * WARNING: This should be disabled in production!
 * Consider adding @Profile("dev") or @ConditionalOnProperty
 */
@RestController
@RequestMapping("/api/debug/circuit-breaker")
public class CircuitBreakerDebugController {

    @Autowired
    private HystrixUserService hystrixUserService;

    @Autowired
    private HystrixProductService hystrixProductService;

    // Flag to simulate failures
    private boolean simulateUserServiceFailure = false;
    private boolean simulateProductServiceFailure = false;

    /**
     * Get circuit breaker status for all commands
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Get metrics for UserService commands
        status.put("userService", getCommandMetrics("SearchUsers", "GetUserById", "GetFollowing", "GetFollowers"));
        
        // Get metrics for ProductService commands
        status.put("productService", getCommandMetrics("GetAllProducts", "GetProductById", "SearchProducts"));
        
        // Simulation flags
        Map<String, Boolean> simulation = new HashMap<>();
        simulation.put("userServiceFailure", simulateUserServiceFailure);
        simulation.put("productServiceFailure", simulateProductServiceFailure);
        status.put("simulation", simulation);
        
        return ResponseEntity.ok(status);
    }

    /**
     * Toggle failure simulation for UserService
     */
    @PostMapping("/simulate/user-service-failure")
    public ResponseEntity<Map<String, Object>> toggleUserServiceFailure() {
        simulateUserServiceFailure = !simulateUserServiceFailure;
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", simulateUserServiceFailure);
        response.put("message", simulateUserServiceFailure 
            ? "UserService failures will be simulated" 
            : "UserService failures disabled");
        return ResponseEntity.ok(response);
    }

    /**
     * Toggle failure simulation for ProductService
     */
    @PostMapping("/simulate/product-service-failure")
    public ResponseEntity<Map<String, Object>> toggleProductServiceFailure() {
        simulateProductServiceFailure = !simulateProductServiceFailure;
        Map<String, Object> response = new HashMap<>();
        response.put("enabled", simulateProductServiceFailure);
        response.put("message", simulateProductServiceFailure 
            ? "ProductService failures will be simulated" 
            : "ProductService failures disabled");
        return ResponseEntity.ok(response);
    }

    /**
     * Trigger a test request that will fail (for testing circuit breaker)
     * Note: To actually trigger circuit breaker, failures must occur inside Hystrix command.
     * This endpoint will call the service normally. To see circuit breaker in action,
     * you need actual database failures or modify services to check simulation flags.
     */
    @PostMapping("/test/user-search")
    public ResponseEntity<Map<String, Object>> testUserSearch(
            @RequestParam(defaultValue = "test") String query,
            @RequestParam(defaultValue = "false") boolean forceFailure) {
        Map<String, Object> result = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            
            // Call through Hystrix - if simulation is enabled, use a query that will likely fail
            // (e.g., very long query or special marker that service can detect)
            String testQuery = (forceFailure || simulateUserServiceFailure) 
                ? "__SIMULATE_FAILURE__" + query 
                : query;
            
            var users = hystrixUserService.searchUsers(testQuery);
            long duration = System.currentTimeMillis() - startTime;
            
            result.put("success", true);
            result.put("users", users);
            result.put("duration", duration + "ms");
            result.put("circuitOpen", isCircuitOpen("SearchUsers"));
            result.put("note", "If simulation enabled, check if service handles failure marker");
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("circuitOpen", isCircuitOpen("SearchUsers"));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Trigger a test request for products that will fail
     */
    @PostMapping("/test/products")
    public ResponseEntity<Map<String, Object>> testProducts(
            @RequestParam(defaultValue = "false") boolean forceFailure) {
        Map<String, Object> result = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            
            // Set system property to trigger failure in ProductService
            if (forceFailure || simulateProductServiceFailure) {
                System.setProperty("circuit.breaker.simulate.product.failure", "true");
            } else {
                System.clearProperty("circuit.breaker.simulate.product.failure");
            }
            
            var products = hystrixProductService.getAllProducts();
            long duration = System.currentTimeMillis() - startTime;
            
            // Clear the property after test
            System.clearProperty("circuit.breaker.simulate.product.failure");
            
            result.put("success", true);
            result.put("products", products);
            result.put("duration", duration + "ms");
            result.put("circuitOpen", isCircuitOpen("GetAllProducts"));
        } catch (Exception e) {
            // Clear the property even on failure
            System.clearProperty("circuit.breaker.simulate.product.failure");
            
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("circuitOpen", isCircuitOpen("GetAllProducts"));
        }
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> getCommandMetrics(String... commandNames) {
        Map<String, Object> metrics = new HashMap<>();
        
        for (String commandName : commandNames) {
            HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(commandName);
            HystrixCommandMetrics commandMetrics = HystrixCommandMetrics.getInstance(commandKey);
            
            if (commandMetrics != null) {
                Map<String, Object> commandData = new HashMap<>();
                
                // Get actual circuit breaker state
                // Note: HystrixCircuitBreaker is associated with command key
                // We need to construct it with the same properties as the command
                com.netflix.hystrix.HystrixCircuitBreaker circuitBreaker = 
                    com.netflix.hystrix.HystrixCircuitBreaker.Factory.getInstance(commandKey);
                
                boolean circuitOpen = circuitBreaker != null && circuitBreaker.isOpen();
                
                long totalRequests = commandMetrics.getHealthCounts().getTotalRequests();
                long errorCount = commandMetrics.getHealthCounts().getErrorCount();
                int errorPercentage = commandMetrics.getHealthCounts().getErrorPercentage();
                
                commandData.put("circuitOpen", circuitOpen);
                commandData.put("totalRequests", totalRequests);
                commandData.put("errorCount", errorCount);
                commandData.put("errorPercentage", errorPercentage);
                commandData.put("currentConcurrentExecutionCount", commandMetrics.getCurrentConcurrentExecutionCount());
                
                metrics.put(commandName, commandData);
            } else {
                Map<String, Object> commandData = new HashMap<>();
                commandData.put("circuitOpen", false);
                commandData.put("totalRequests", 0);
                commandData.put("errorCount", 0);
                commandData.put("errorPercentage", 0);
                commandData.put("status", "No metrics available yet");
                metrics.put(commandName, commandData);
            }
        }
        
        return metrics;
    }

    private boolean isCircuitOpen(String commandName) {
        HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(commandName);
        
        com.netflix.hystrix.HystrixCircuitBreaker circuitBreaker = 
            com.netflix.hystrix.HystrixCircuitBreaker.Factory.getInstance(commandKey);
        
        return circuitBreaker != null && circuitBreaker.isOpen();
    }

    public boolean shouldSimulateUserServiceFailure() {
        return simulateUserServiceFailure;
    }

    public boolean shouldSimulateProductServiceFailure() {
        return simulateProductServiceFailure;
    }
}

