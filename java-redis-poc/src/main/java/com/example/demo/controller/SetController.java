package com.example.demo.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.SetService;

/**
 * Demonstrates Redis Set operations through a tags/interests use case.
 * Each user has a set of interest tags. The key power of Redis Sets is
 * set algebra — intersection, difference, and union — which enables
 * finding common interests, unique interests, and combined interests
 * between users in O(N) time.
 */
@RestController
@RequestMapping("/api/tags")
public class SetController {

    private final SetService setService;

    public SetController(SetService setService) {
        this.setService = setService;
    }

    /**
     * Add a tag to a user's interests.
     * Example: POST /api/tags/alice with body "java"
     */
    @PostMapping("/{userId}")
    public ResponseEntity<Void> addTag(@PathVariable String userId, @RequestBody String tag) {
        setService.addTag(userId, tag.trim());
        return ResponseEntity.ok().build();
    }

    /**
     * Get all tags for a user (SMEMBERS).
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Set<Object>> getTags(@PathVariable String userId) {
        return ResponseEntity.ok(setService.getTags(userId));
    }

    /**
     * Check if a user has a specific tag (SISMEMBER).
     */
    @GetMapping("/{userId}/has/{tag}")
    public ResponseEntity<Map<String, Boolean>> hasTag(@PathVariable String userId, @PathVariable String tag) {
        return ResponseEntity.ok(Map.of("isMember", setService.hasTag(userId, tag)));
    }

    /**
     * Remove a tag from a user (SREM).
     */
    @DeleteMapping("/{userId}/{tag}")
    public ResponseEntity<Void> removeTag(@PathVariable String userId, @PathVariable String tag) {
        setService.removeTag(userId, tag);
        return ResponseEntity.ok().build();
    }

    /**
     * Find common tags between two users (SINTER).
     * Example: GET /api/tags/alice/common/bob
     */
    @GetMapping("/{userId1}/common/{userId2}")
    public ResponseEntity<Set<Object>> commonTags(@PathVariable String userId1, @PathVariable String userId2) {
        return ResponseEntity.ok(setService.commonTags(userId1, userId2));
    }

    /**
     * Find tags unique to userId1 that userId2 doesn't have (SDIFF).
     * Useful for recommendations.
     * Example: GET /api/tags/alice/unique/bob
     */
    @GetMapping("/{userId1}/unique/{userId2}")
    public ResponseEntity<Set<Object>> uniqueTags(@PathVariable String userId1, @PathVariable String userId2) {
        return ResponseEntity.ok(setService.uniqueTags(userId1, userId2));
    }

    /**
     * Combine all tags from both users (SUNION).
     * Example: GET /api/tags/alice/union/bob
     */
    @GetMapping("/{userId1}/union/{userId2}")
    public ResponseEntity<Set<Object>> allTags(@PathVariable String userId1, @PathVariable String userId2) {
        return ResponseEntity.ok(setService.allTags(userId1, userId2));
    }

    /**
     * Get tag count for a user (SCARD).
     */
    @GetMapping("/{userId}/count")
    public ResponseEntity<Map<String, Long>> tagCount(@PathVariable String userId) {
        return ResponseEntity.ok(Map.of("count", setService.tagCount(userId)));
    }
}
