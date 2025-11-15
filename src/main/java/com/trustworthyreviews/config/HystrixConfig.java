package com.trustworthyreviews.config;

import com.netflix.hystrix.HystrixCommandProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hystrix Configuration
 * 
 * NOTE: Hystrix is in maintenance mode. For new projects, consider using Resilience4j instead,
 * which is actively maintained and has better Spring Boot 3.x support.
 */
@Configuration
public class HystrixConfig {

    /**
     * Configure Hystrix properties for database operations
     */
    @Bean
    public HystrixCommandProperties.Setter databaseCommandProperties() {
        return HystrixCommandProperties.Setter()
                .withCircuitBreakerEnabled(true)
                .withCircuitBreakerRequestVolumeThreshold(10) // Open circuit after 10 requests
                .withCircuitBreakerErrorThresholdPercentage(50) // Open if 50% fail
                .withCircuitBreakerSleepWindowInMilliseconds(5000) // Try again after 5 seconds
                .withExecutionTimeoutEnabled(true)
                .withExecutionTimeoutInMilliseconds(3000) // 3 second timeout for DB calls
                .withFallbackEnabled(true);
    }

    /**
     * Configure Hystrix properties for external HTTP calls
     */
    @Bean
    public HystrixCommandProperties.Setter httpCommandProperties() {
        return HystrixCommandProperties.Setter()
                .withCircuitBreakerEnabled(true)
                .withCircuitBreakerRequestVolumeThreshold(5) // Open circuit after 5 requests
                .withCircuitBreakerErrorThresholdPercentage(50) // Open if 50% fail
                .withCircuitBreakerSleepWindowInMilliseconds(10000) // Try again after 10 seconds
                .withExecutionTimeoutEnabled(true)
                .withExecutionTimeoutInMilliseconds(5000) // 5 second timeout for HTTP calls
                .withFallbackEnabled(true);
    }
}

