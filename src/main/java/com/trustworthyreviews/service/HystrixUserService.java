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
}

