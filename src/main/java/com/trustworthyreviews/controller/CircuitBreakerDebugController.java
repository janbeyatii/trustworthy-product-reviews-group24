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
 * Debug controller for testing and visualizing Hystrix circuit breaker behavior.
 * 
 * This controller provides REST endpoints to:
 * - Monitor circuit breaker status and metrics in real-time
 * - Simulate failures to test circuit breaker behavior
 * - Trigger test requests to observe circuit breaker responses
 * 
 * Circuit Breaker Behavior:
 * - Opens when: 10+ requests with 50%+ error rate
 * - Sleep window: 5 seconds (circuit stays open for 5s before attempting recovery)
 * - Timeout: 3 seconds for database operations
 * 
 * WARNING: This should be disabled in production!
 * Consider adding @Profile("dev") or @ConditionalOnProperty("circuit.breaker.debug.enabled")
 * 
 * @author Trustworthy Reviews Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/debug/circuit-breaker")
public class CircuitBreakerDebugController {

    /** Hystrix-wrapped UserService for circuit breaker protection */
    @Autowired
    private HystrixUserService hystrixUserService;

    /** Hystrix-wrapped ProductService for circuit breaker protection */
    @Autowired
    private HystrixProductService hystrixProductService;

    /**
     * Flag to enable/disable failure simulation for UserService.
     * When enabled, test requests will include a failure marker that causes exceptions.
     */
    private boolean simulateUserServiceFailure = false;
    
    /**
     * Flag to enable/disable failure simulation for ProductService.
     * When enabled, test requests will trigger exceptions via system property.
     */
    private boolean simulateProductServiceFailure = false;

    /**
     * GET /api/debug/circuit-breaker/status
     * 
     * Retrieves the current status and metrics for all circuit breakers.
     * 
     * Response includes:
     * - Circuit breaker state (OPEN/CLOSED) for each command
     * - Total requests, error count, error percentage
     * - Current concurrent execution count
     * - Simulation flags status
     * 
     * @return Map containing circuit breaker metrics for UserService and ProductService commands
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Get metrics for all UserService Hystrix commands
        // Command names must match the HystrixCommandKey defined in HystrixUserService
        status.put("userService", getCommandMetrics("SearchUsers", "GetUserById", "GetFollowing", "GetFollowers"));
        
        // Get metrics for all ProductService Hystrix commands
        // Command names must match the HystrixCommandKey defined in HystrixProductService
        status.put("productService", getCommandMetrics("GetAllProducts", "GetProductById", "SearchProducts"));
        
        // Include current simulation flags so frontend knows if failures are enabled
        Map<String, Boolean> simulation = new HashMap<>();
        simulation.put("userServiceFailure", simulateUserServiceFailure);
        simulation.put("productServiceFailure", simulateProductServiceFailure);
        status.put("simulation", simulation);
        
        return ResponseEntity.ok(status);
    }

    /**
     * POST /api/debug/circuit-breaker/simulate/user-service-failure
     * 
     * Toggles failure simulation for UserService.
     * When enabled, test requests will include a special marker ("__SIMULATE_FAILURE__")
     * that causes UserService.searchUsers() to throw an exception, triggering the circuit breaker.
     * 
     * @return Response indicating whether simulation is now enabled or disabled
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
     * POST /api/debug/circuit-breaker/simulate/product-service-failure
     * 
     * Toggles failure simulation for ProductService.
     * When enabled, test requests will set a system property that causes
     * ProductService.getAllProducts() to throw an exception, triggering the circuit breaker.
     * 
     * @return Response indicating whether simulation is now enabled or disabled
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
     * POST /api/debug/circuit-breaker/test/user-search
     * 
     * Triggers a test request to UserService.searchUsers() through the Hystrix circuit breaker.
     * This allows testing circuit breaker behavior by making actual service calls.
     * 
     * Failure Simulation:
     * - If forceFailure=true or simulateUserServiceFailure=true, the query will be prefixed
     *   with "__SIMULATE_FAILURE__" which causes UserService to throw an exception.
     * - The exception occurs inside the Hystrix command, so it's properly tracked by the circuit breaker.
     * 
     * @param query The search query to use (default: "test")
     * @param forceFailure If true, force a failure for this request (overrides simulation flag)
     * @return Map containing:
     *   - success: Whether the request succeeded
     *   - users: List of users returned (empty if circuit open or failure)
     *   - duration: Request duration in milliseconds
     *   - circuitOpen: Current state of the circuit breaker
     *   - error: Error message if request failed
     */
    @PostMapping("/test/user-search")
    public ResponseEntity<Map<String, Object>> testUserSearch(
            @RequestParam(defaultValue = "test") String query,
            @RequestParam(defaultValue = "false") boolean forceFailure) {
        Map<String, Object> result = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            
            // If failure simulation is enabled, prefix query with special marker
            // UserService.searchUsers() checks for this marker and throws an exception
            String testQuery = (forceFailure || simulateUserServiceFailure) 
                ? "__SIMULATE_FAILURE__" + query 
                : query;
            
            // Call through Hystrix - this will be tracked by circuit breaker
            var users = hystrixUserService.searchUsers(testQuery);
            long duration = System.currentTimeMillis() - startTime;
            
            result.put("success", true);
            result.put("users", users);
            result.put("duration", duration + "ms");
            result.put("circuitOpen", isCircuitOpen("SearchUsers"));
            result.put("note", "If simulation enabled, check if service handles failure marker");
        } catch (Exception e) {
            // Exception was caught by Hystrix - circuit breaker will track this failure
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("circuitOpen", isCircuitOpen("SearchUsers"));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/debug/circuit-breaker/test/products
     * 
     * Triggers a test request to ProductService.getAllProducts() through the Hystrix circuit breaker.
     * This allows testing circuit breaker behavior by making actual service calls.
     * 
     * Failure Simulation:
     * - If forceFailure=true or simulateProductServiceFailure=true, sets a system property
     *   "circuit.breaker.simulate.product.failure" to "true"
     * - ProductService.getAllProducts() checks this property and throws an exception if set
     * - The exception occurs inside the Hystrix command, so it's properly tracked
     * - Property is cleared after the request completes (success or failure)
     * 
     * @param forceFailure If true, force a failure for this request (overrides simulation flag)
     * @return Map containing:
     *   - success: Whether the request succeeded
     *   - products: List of products returned (empty if circuit open or failure)
     *   - duration: Request duration in milliseconds
     *   - circuitOpen: Current state of the circuit breaker
     *   - error: Error message if request failed
     */
    @PostMapping("/test/products")
    public ResponseEntity<Map<String, Object>> testProducts(
            @RequestParam(defaultValue = "false") boolean forceFailure) {
        Map<String, Object> result = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            
            // Set system property to trigger failure in ProductService
            // ProductService.getAllProducts() checks this property and throws if it's "true"
            if (forceFailure || simulateProductServiceFailure) {
                System.setProperty("circuit.breaker.simulate.product.failure", "true");
            } else {
                System.clearProperty("circuit.breaker.simulate.product.failure");
            }
            
            // Call through Hystrix - this will be tracked by circuit breaker
            var products = hystrixProductService.getAllProducts();
            long duration = System.currentTimeMillis() - startTime;
            
            // Always clear the property after test to avoid affecting other requests
            System.clearProperty("circuit.breaker.simulate.product.failure");
            
            result.put("success", true);
            result.put("products", products);
            result.put("duration", duration + "ms");
            result.put("circuitOpen", isCircuitOpen("GetAllProducts"));
        } catch (Exception e) {
            // Clear the property even on failure to ensure clean state
            System.clearProperty("circuit.breaker.simulate.product.failure");
            
            // Exception was caught by Hystrix - circuit breaker will track this failure
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("circuitOpen", isCircuitOpen("GetAllProducts"));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves metrics for the specified Hystrix commands.
     * 
     * For each command, collects:
     * - Circuit breaker state (OPEN/CLOSED)
     * - Total request count
     * - Error count and percentage
     * - Current concurrent executions
     * 
     * @param commandNames Array of Hystrix command names (must match HystrixCommandKey names)
     * @return Map where keys are command names and values are metric maps
     */
    private Map<String, Object> getCommandMetrics(String... commandNames) {
        Map<String, Object> metrics = new HashMap<>();
        
        for (String commandName : commandNames) {
            // Get the Hystrix command key for this command
            HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(commandName);
            
            // Retrieve metrics instance for this command (null if command hasn't been executed yet)
            HystrixCommandMetrics commandMetrics = HystrixCommandMetrics.getInstance(commandKey);
            
            if (commandMetrics != null) {
                Map<String, Object> commandData = new HashMap<>();
                
                // Get the actual circuit breaker instance for this command
                // The circuit breaker tracks whether it's open or closed
                com.netflix.hystrix.HystrixCircuitBreaker circuitBreaker = 
                    com.netflix.hystrix.HystrixCircuitBreaker.Factory.getInstance(commandKey);
                
                // Check if circuit is currently open (blocking requests)
                boolean circuitOpen = circuitBreaker != null && circuitBreaker.isOpen();
                
                // Get health metrics from Hystrix
                long totalRequests = commandMetrics.getHealthCounts().getTotalRequests();
                long errorCount = commandMetrics.getHealthCounts().getErrorCount();
                int errorPercentage = commandMetrics.getHealthCounts().getErrorPercentage();
                
                // Build response data
                commandData.put("circuitOpen", circuitOpen);
                commandData.put("totalRequests", totalRequests);
                commandData.put("errorCount", errorCount);
                commandData.put("errorPercentage", errorPercentage);
                commandData.put("currentConcurrentExecutionCount", commandMetrics.getCurrentConcurrentExecutionCount());
                
                metrics.put(commandName, commandData);
            } else {
                // Command hasn't been executed yet, return default values
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

    /**
     * Checks if the circuit breaker for a specific command is currently open.
     * 
     * Circuit is open when:
     * - 10+ requests have been made
     * - Error rate exceeds 50%
     * - Circuit stays open for 5 seconds (sleep window) before attempting recovery
     * 
     * @param commandName The Hystrix command name to check
     * @return true if circuit is open, false if closed or command doesn't exist
     */
    private boolean isCircuitOpen(String commandName) {
        HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(commandName);
        
        // Get the circuit breaker instance for this command
        com.netflix.hystrix.HystrixCircuitBreaker circuitBreaker = 
            com.netflix.hystrix.HystrixCircuitBreaker.Factory.getInstance(commandKey);
        
        // Return true if circuit breaker exists and is open
        return circuitBreaker != null && circuitBreaker.isOpen();
    }

    /**
     * Getter for UserService failure simulation flag.
     * Used by other components to check if failures should be simulated.
     * 
     * @return true if UserService failure simulation is enabled
     */
    public boolean shouldSimulateUserServiceFailure() {
        return simulateUserServiceFailure;
    }

    /**
     * Getter for ProductService failure simulation flag.
     * Used by other components to check if failures should be simulated.
     * 
     * @return true if ProductService failure simulation is enabled
     */
    public boolean shouldSimulateProductServiceFailure() {
        return simulateProductServiceFailure;
    }
}

