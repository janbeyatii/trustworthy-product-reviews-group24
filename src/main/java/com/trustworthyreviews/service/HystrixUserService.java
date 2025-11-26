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
 * Hystrix-wrapped UserService methods for circuit breaker protection
 */
@Service
public class HystrixUserService {

    private static final Logger log = LoggerFactory.getLogger(HystrixUserService.class);
    
    private final UserService userService;

    public HystrixUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Search users with circuit breaker protection
     */
    public List<Map<String, Object>> searchUsers(String query) {
        return new SearchUsersCommand(query, userService).execute();
    }

    /**
     * Get user by ID with circuit breaker protection
     */
    public Map<String, Object> getUserById(String userId) {
        return new GetUserByIdCommand(userId, userService).execute();
    }

    /**
     * Get following users with circuit breaker protection
     */
    public List<Map<String, Object>> getFollowingForUser(String userId) {
        return new GetFollowingCommand(userId, userService).execute();
    }

    /**
     * Get followers with circuit breaker protection
     */
    public List<Map<String, Object>> getFollowersForUser(String userId) {
        return new GetFollowersCommand(userId, userService).execute();
    }

    public List<Map<String, Object>> findSimilarUsers(String userId, int limit, double minSimilarity) {
        return new FindSimilarUsersCommand(userId, limit, minSimilarity, userService).execute();
    }

    public double calculateCombinedJaccardSimilarity(String userId1, String userId2) {
        return new CalculateCombinedSimilarityCommand(userId1, userId2, userService).execute();
    }

    // Hystrix Commands
    
    private static class SearchUsersCommand extends HystrixCommand<List<Map<String, Object>>> {
        private final String query;
        private final UserService userService;

        protected SearchUsersCommand(String query, UserService userService) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Database"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("SearchUsers"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                            .withCircuitBreakerEnabled(true)
                            .withCircuitBreakerRequestVolumeThreshold(10)
                            .withCircuitBreakerErrorThresholdPercentage(50)
                            .withCircuitBreakerSleepWindowInMilliseconds(5000)
                            .withExecutionTimeoutInMilliseconds(3000)
                            .withFallbackEnabled(true)));
            this.query = query;
            this.userService = userService;
        }

        @Override
        protected List<Map<String, Object>> run() throws Exception {
            return userService.searchUsers(query);
        }

        @Override
        protected List<Map<String, Object>> getFallback() {
            log.warn("SearchUsers circuit breaker opened or timed out. Returning empty list.");
            return Collections.emptyList();
        }
    }

    private static class GetUserByIdCommand extends HystrixCommand<Map<String, Object>> {
        private final String userId;
        private final UserService userService;

        protected GetUserByIdCommand(String userId, UserService userService) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Database"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("GetUserById"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                            .withCircuitBreakerEnabled(true)
                            .withCircuitBreakerRequestVolumeThreshold(10)
                            .withCircuitBreakerErrorThresholdPercentage(50)
                            .withCircuitBreakerSleepWindowInMilliseconds(5000)
                            .withExecutionTimeoutInMilliseconds(3000)
                            .withFallbackEnabled(true)));
            this.userId = userId;
            this.userService = userService;
        }

        @Override
        protected Map<String, Object> run() throws Exception {
            return userService.getUserById(userId);
        }

        @Override
        protected Map<String, Object> getFallback() {
            log.warn("GetUserById circuit breaker opened or timed out for user {}. Returning null.", userId);
            return null;
        }
    }

    private static class GetFollowingCommand extends HystrixCommand<List<Map<String, Object>>> {
        private final String userId;
        private final UserService userService;

        protected GetFollowingCommand(String userId, UserService userService) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Database"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("GetFollowing"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                            .withCircuitBreakerEnabled(true)
                            .withCircuitBreakerRequestVolumeThreshold(10)
                            .withCircuitBreakerErrorThresholdPercentage(50)
                            .withCircuitBreakerSleepWindowInMilliseconds(5000)
                            .withExecutionTimeoutInMilliseconds(3000)
                            .withFallbackEnabled(true)));
            this.userId = userId;
            this.userService = userService;
        }

        @Override
        protected List<Map<String, Object>> run() throws Exception {
            return userService.getFollowingForUser(userId);
        }

        @Override
        protected List<Map<String, Object>> getFallback() {
            log.warn("GetFollowing circuit breaker opened or timed out for user {}. Returning empty list.", userId);
            return Collections.emptyList();
        }
    }

    private static class GetFollowersCommand extends HystrixCommand<List<Map<String, Object>>> {
        private final String userId;
        private final UserService userService;

        protected GetFollowersCommand(String userId, UserService userService) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Database"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("GetFollowers"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                            .withCircuitBreakerEnabled(true)
                            .withCircuitBreakerRequestVolumeThreshold(10)
                            .withCircuitBreakerErrorThresholdPercentage(50)
                            .withCircuitBreakerSleepWindowInMilliseconds(5000)
                            .withExecutionTimeoutInMilliseconds(3000)
                            .withFallbackEnabled(true)));
            this.userId = userId;
            this.userService = userService;
        }

        @Override
        protected List<Map<String, Object>> run() throws Exception {
            return userService.getFollowersForUser(userId);
        }

        @Override
        protected List<Map<String, Object>> getFallback() {
            log.warn("GetFollowers circuit breaker opened or timed out for user {}. Returning empty list.", userId);
            return Collections.emptyList();
        }
    }

    private static class FindSimilarUsersCommand extends HystrixCommand<List<Map<String, Object>>> {
        private final String userId;
        private final int limit;
        private final double minSimilarity;
        private final UserService userService;

        protected FindSimilarUsersCommand(String userId, int limit, double minSimilarity, UserService userService) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Database"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("FindSimilarUsers"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                            .withCircuitBreakerEnabled(true)
                            .withCircuitBreakerRequestVolumeThreshold(10)
                            .withCircuitBreakerErrorThresholdPercentage(50)
                            .withCircuitBreakerSleepWindowInMilliseconds(5000)
                            .withExecutionTimeoutInMilliseconds(10000) // Longer timeout for similarity calculation
                            .withFallbackEnabled(true)));
            this.userId = userId;
            this.limit = limit;
            this.minSimilarity = minSimilarity;
            this.userService = userService;
        }

        @Override
        protected List<Map<String, Object>> run() throws Exception {
            return userService.findSimilarUsers(userId, limit, minSimilarity);
        }

        @Override
        protected List<Map<String, Object>> getFallback() {
            log.warn("FindSimilarUsers circuit breaker opened or timed out for user {}. Returning empty list.", userId);
            return Collections.emptyList();
        }
    }

    private static class CalculateCombinedSimilarityCommand extends HystrixCommand<Double> {
        private final String userId1;
        private final String userId2;
        private final UserService userService;

        protected CalculateCombinedSimilarityCommand(String userId1, String userId2, UserService userService) {
            super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Database"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey("CalculateCombinedSimilarity"))
                    .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                            .withCircuitBreakerEnabled(true)
                            .withCircuitBreakerRequestVolumeThreshold(10)
                            .withCircuitBreakerErrorThresholdPercentage(50)
                            .withCircuitBreakerSleepWindowInMilliseconds(5000)
                            .withExecutionTimeoutInMilliseconds(5000)
                            .withFallbackEnabled(true)));
            this.userId1 = userId1;
            this.userId2 = userId2;
            this.userService = userService;
        }

        @Override
        protected Double run() throws Exception {
            return userService.calculateCombinedJaccardSimilarity(userId1, userId2);
        }

        @Override
        protected Double getFallback() {
            log.warn("CalculateCombinedSimilarity circuit breaker opened or timed out for users {} and {}. Returning 0.0.", userId1, userId2);
            return 0.0;
        }
    }
}

