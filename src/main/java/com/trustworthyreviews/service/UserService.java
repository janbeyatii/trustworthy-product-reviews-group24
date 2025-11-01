package com.trustworthyreviews.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Search users by display name or email
     * This queries Supabase auth.users table through the raw database connection
     */
    public List<Map<String, Object>> searchUsers(String query) {
        try {
            // Query auth.users table to find users matching the query
            String sql = """
                SELECT 
                    id,
                    email,
                    raw_user_meta_data,
                    created_at
                FROM auth.users
                WHERE 
                    email ILIKE ?
                ORDER BY email
                LIMIT 20
            """;
            
            String pattern = "%" + query + "%";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, pattern);
            
            // Extract display_name from JSON metadata
            users.forEach(user -> {
                if (user.get("raw_user_meta_data") != null) {
                    try {
                        Object metadata = user.get("raw_user_meta_data");
                        if (metadata instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> metaMap = (Map<String, Object>) metadata;
                            user.put("display_name", metaMap.get("display_name"));
                        }
                    } catch (Exception e) {
                        user.put("display_name", null);
                    }
                }
            });
            
            return users;
        } catch (Exception e) {
            throw new RuntimeException("Error searching users: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by UUID
     */
    public Map<String, Object> getUserById(String userId) {
        try {
            log.info("Fetching user by ID: {}", userId);
            
            String sql = """
                SELECT 
                    id,
                    email,
                    created_at
                FROM auth.users
                WHERE id = ?
            """;
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
            
            if (results.isEmpty()) {
                log.warn("No user found with ID: {}", userId);
                return null;
            }
            
            log.info("Successfully fetched user: {}", results.get(0));
            return results.get(0);
        } catch (Exception e) {
            log.error("Error fetching user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error fetching user: " + e.getMessage(), e);
        }
    }
}

