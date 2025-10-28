package com.trustworthyreviews.service;

import com.trustworthyreviews.model.Review;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ReviewService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Review> findAll() {
        String sql = "SELECT id, product_id AS productId, user_id AS userId, rating, content, product_url AS productUrl, created_at AS createdAt, updated_at AS updatedAt FROM reviews ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Review.class));
    }

    public List<Review> findByUser(String userId) {
        String sql = "SELECT id, product_id AS productId, user_id AS userId, rating, content, product_url AS productUrl, created_at AS createdAt, updated_at AS updatedAt FROM reviews WHERE user_id = :userId ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("userId", userId), new BeanPropertyRowMapper<>(Review.class));
    }

    public Optional<Review> findById(Long id) {
        String sql = "SELECT id, product_id AS productId, user_id AS userId, rating, content, product_url AS productUrl, created_at AS createdAt, updated_at AS updatedAt FROM reviews WHERE id = :id";
        List<Review> results = jdbcTemplate.query(sql, new MapSqlParameterSource("id", id), new BeanPropertyRowMapper<>(Review.class));
        return results.stream().findFirst();
    }

    public Review create(Review review) {
        String sql = "INSERT INTO reviews (product_id, user_id, rating, content, product_url) VALUES (:productId, :userId, :rating, :content, :productUrl)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productId", review.getProductId())
                .addValue("userId", review.getUserId())
                .addValue("rating", review.getRating())
                .addValue("content", review.getContent())
                .addValue("productUrl", review.getProductUrl());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key != null) {
            review.setId(key.longValue());
        }
        return review;
    }

    public Review update(Long id, Review review) {
        String sql = "UPDATE reviews SET rating = :rating, content = :content, product_url = :productUrl, updated_at = NOW() WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("rating", review.getRating())
                .addValue("content", review.getContent())
                .addValue("productUrl", review.getProductUrl());
        int rows = jdbcTemplate.update(sql, params);
        if (rows == 0) {
            throw new DataAccessException("No review updated for id " + id) {
            };
        }
        review.setId(id);
        return review;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM reviews WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        jdbcTemplate.update(sql, params);
    }
}
