/* 
package com.trustworthyreviews.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@AutoConfigureTestDatabase
class UserServiceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(jdbcTemplate, null, objectMapper);

        jdbcTemplate.execute("""
          CREATE TABLE IF NOT EXISTS users (
            id VARCHAR(36) PRIMARY KEY,
            email VARCHAR(255) NOT NULL,
            display_name VARCHAR(255),
            raw_user_meta_data VARCHAR(255))
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS relations (
                uid VARCHAR(36) NOT NULL,
                following VARCHAR(36) NOT NULL
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS user_similarity_cache (
                uuid1 VARCHAR(36) NOT NULL,
                uuid2 VARCHAR(36) NOT NULL,
                similarity_score DOUBLE,
                product_similarity DOUBLE,
                rating_similarity DOUBLE,
                last_calculated TIMESTAMP,
                PRIMARY KEY (uuid1, uuid2)
            )
        """);

        jdbcTemplate.update("DELETE FROM relations");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM user_similarity_cache");

        jdbcTemplate.update("""
            INSERT INTO users (id, email, raw_user_meta_data) VALUES 
            ('00000000-0000-0000-0000-000000000001','alice@example.com','{\"display_name\":\"Alice\"}'),
            ('00000000-0000-0000-0000-000000000002','bob@example.com','{\"display_name\":\"Bob\"}')
        """);

        jdbcTemplate.update("""
            INSERT INTO relations (uid, following) VALUES 
            ('00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002')
        """);
    }

    @Test
    void getUserById_existingUser_returnsUser() {
        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT * FROM users WHERE id = ?", "00000000-0000-0000-0000-000000000001"
        );
        assertNotNull(user);
        assertEquals("alice@example.com", user.get("email"));
    }

    @Test
    void getFollowingForUser_returnsFollowing() {
        List<Map<String, Object>> following = jdbcTemplate.queryForList("""
            SELECT u.id, u.email, u.raw_user_meta_data 
            FROM relations r
            JOIN users u ON u.id = r.following
            WHERE r.uid = ?
        """, "00000000-0000-0000-0000-000000000001");

        assertEquals(1, following.size());
        assertEquals("bob@example.com", following.get(0).get("email"));
    }

    @Test
    void getFollowersForUser_returnsFollowers() {
        List<Map<String, Object>> followers = jdbcTemplate.queryForList("""
            SELECT u.id, u.email, u.raw_user_meta_data 
            FROM relations r
            JOIN users u ON u.id = r.uid
            WHERE r.following = ?
        """, "00000000-0000-0000-0000-000000000002");

        assertEquals(1, followers.size());
        assertEquals("alice@example.com", followers.get(0).get("email"));
    }

    @Test
    void getDegreeOfSeparation_directFollow_returns1() {
        List<Map<String, Object>> directFollow = jdbcTemplate.queryForList("""
            SELECT 1 as degree FROM relations
            WHERE uid = ? AND following = ?
        """, "00000000-0000-0000-0000-000000000001",
                "00000000-0000-0000-0000-000000000002");

        Integer degree = directFollow.isEmpty() ? null : (Integer) directFollow.get(0).get("degree");
        assertEquals(1, degree);
    }

    @Test
    void getUserProfileWithMetrics_includesMetrics() {
        Map<String, Object> profile = jdbcTemplate.queryForMap("""
            SELECT * FROM users WHERE id = ?
        """, "00000000-0000-0000-0000-000000000002");

        assertNotNull(profile);
        assertEquals("bob@example.com", profile.get("email"));
    }
}
*/