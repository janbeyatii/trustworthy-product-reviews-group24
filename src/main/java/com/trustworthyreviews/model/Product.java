package com.trustworthyreviews.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Product {
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must be fewer than 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must be fewer than 1000 characters")
    private String description;

    @Size(max = 2048, message = "Product URL must be fewer than 2048 characters")
    private String productUrl;

    private BigDecimal averageRating;
    private OffsetDateTime createdAt;

    public Product() {
    }

    public Product(Long id, String name, String description, String productUrl, BigDecimal averageRating, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.productUrl = productUrl;
        this.averageRating = averageRating;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
