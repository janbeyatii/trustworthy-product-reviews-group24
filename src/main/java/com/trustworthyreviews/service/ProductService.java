package com.trustworthyreviews.service;

import com.trustworthyreviews.model.Product;
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
public class ProductService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProductService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Product> findAll() {
        String sql = "SELECT id, name, description, product_url AS productUrl, average_rating AS averageRating, created_at AS createdAt FROM products ORDER BY name";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Product.class));
    }

    public Optional<Product> findById(Long id) {
        String sql = "SELECT id, name, description, product_url AS productUrl, average_rating AS averageRating, created_at AS createdAt FROM products WHERE id = :id";
        List<Product> results = jdbcTemplate.query(sql, new MapSqlParameterSource("id", id), new BeanPropertyRowMapper<>(Product.class));
        return results.stream().findFirst();
    }

    public Product create(Product product) {
        String sql = "INSERT INTO products (name, description, product_url) VALUES (:name, :description, :productUrl)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", product.getName())
                .addValue("description", product.getDescription())
                .addValue("productUrl", product.getProductUrl());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key != null) {
            product.setId(key.longValue());
        }
        return product;
    }

    public Product update(Long id, Product product) {
        String sql = "UPDATE products SET name = :name, description = :description, product_url = :productUrl WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", product.getName())
                .addValue("description", product.getDescription())
                .addValue("productUrl", product.getProductUrl());
        int rows = jdbcTemplate.update(sql, params);
        if (rows == 0) {
            throw new DataAccessException("No product updated for id " + id) {
            };
        }
        product.setId(id);
        return product;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM products WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        jdbcTemplate.update(sql, params);
    }
}
