package com.trustworthyreviews.controller;

import com.trustworthyreviews.model.FollowRelation;
import com.trustworthyreviews.service.FollowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follow")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> follow(@Valid @RequestBody FollowRelation relation) {
        followService.follow(relation.getFollowerId(), relation.getFolloweeId());
        return ResponseEntity.ok(Map.of("status", "followed"));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> unfollow(@Valid @RequestBody FollowRelation relation) {
        followService.unfollow(relation.getFollowerId(), relation.getFolloweeId());
        return ResponseEntity.ok(Map.of("status", "unfollowed"));
    }

    @GetMapping("/followers/{userId}")
    public ResponseEntity<List<FollowRelation>> followers(@PathVariable String userId) {
        return ResponseEntity.ok(followService.findFollowers(userId));
    }

    @GetMapping("/following/{userId}")
    public ResponseEntity<List<FollowRelation>> following(@PathVariable String userId) {
        return ResponseEntity.ok(followService.findFollowing(userId));
    }
}
