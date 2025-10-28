package com.trustworthyreviews.model;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

public class FollowRelation {
    @NotBlank(message = "Follower id is required")
    private String followerId;

    @NotBlank(message = "Followee id is required")
    private String followeeId;

    private OffsetDateTime createdAt;

    public FollowRelation() {
    }

    public FollowRelation(String followerId, String followeeId, OffsetDateTime createdAt) {
        this.followerId = followerId;
        this.followeeId = followeeId;
        this.createdAt = createdAt;
    }

    public String getFollowerId() {
        return followerId;
    }

    public void setFollowerId(String followerId) {
        this.followerId = followerId;
    }

    public String getFolloweeId() {
        return followeeId;
    }

    public void setFolloweeId(String followeeId) {
        this.followeeId = followeeId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
