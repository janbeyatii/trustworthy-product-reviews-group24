package com.trustworthyreviews.service;

import com.trustworthyreviews.model.FollowRelation;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FollowService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FollowService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void follow(String followerId, String followeeId) {
        String sql = "INSERT INTO follows (follower_id, followee_id) VALUES (:followerId, :followeeId) ON CONFLICT DO NOTHING";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("followerId", followerId)
                .addValue("followeeId", followeeId);
        jdbcTemplate.update(sql, params);
    }

    public void unfollow(String followerId, String followeeId) {
        String sql = "DELETE FROM follows WHERE follower_id = :followerId AND followee_id = :followeeId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("followerId", followerId)
                .addValue("followeeId", followeeId);
        jdbcTemplate.update(sql, params);
    }

    public List<FollowRelation> findFollowers(String userId) {
        String sql = "SELECT follower_id AS followerId, followee_id AS followeeId, created_at AS createdAt FROM follows WHERE followee_id = :userId";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("userId", userId), new BeanPropertyRowMapper<>(FollowRelation.class));
    }

    public List<FollowRelation> findFollowing(String userId) {
        String sql = "SELECT follower_id AS followerId, followee_id AS followeeId, created_at AS createdAt FROM follows WHERE follower_id = :userId";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("userId", userId), new BeanPropertyRowMapper<>(FollowRelation.class));
    }
}
