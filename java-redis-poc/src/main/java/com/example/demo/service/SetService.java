package com.example.demo.service;

import java.util.Collections;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SetService {

    private static final String PREFIX = "tags:";

    private final RedisTemplate<String, Object> redisTemplate;

    public SetService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Add a tag/interest to a user's set.
     */
    public void addTag(String userId, String tag) {
        redisTemplate.opsForSet().add(PREFIX + userId, tag);
    }

    /**
     * Get all tags/interests for a user.
     */
    public Set<Object> getTags(String userId) {
        Set<Object> result = redisTemplate.opsForSet().members(PREFIX + userId);
        return result != null ? result : Collections.emptySet();
    }

    /**
     * Check if a user has a specific tag.
     */
    public boolean hasTag(String userId, String tag) {
        Boolean result = redisTemplate.opsForSet().isMember(PREFIX + userId, tag);
        return Boolean.TRUE.equals(result);
    }

    /**
     * Remove a tag from a user.
     */
    public void removeTag(String userId, String tag) {
        redisTemplate.opsForSet().remove(PREFIX + userId, tag);
    }

    /**
     * Find common tags between two users (SINTER).
     */
    public Set<Object> commonTags(String userId1, String userId2) {
        Set<Object> result = redisTemplate.opsForSet().intersect(PREFIX + userId1, PREFIX + userId2);
        return result != null ? result : Collections.emptySet();
    }

    /**
     * Find tags that userId1 has but userId2 doesn't (SDIFF).
     * Useful for recommendations: "topics you follow that they don't."
     */
    public Set<Object> uniqueTags(String userId1, String userId2) {
        Set<Object> result = redisTemplate.opsForSet().difference(PREFIX + userId1, PREFIX + userId2);
        return result != null ? result : Collections.emptySet();
    }

    /**
     * Combine all tags from both users (SUNION).
     */
    public Set<Object> allTags(String userId1, String userId2) {
        Set<Object> result = redisTemplate.opsForSet().union(PREFIX + userId1, PREFIX + userId2);
        return result != null ? result : Collections.emptySet();
    }

    /**
     * Get the number of tags a user has (SCARD).
     */
    public long tagCount(String userId) {
        Long size = redisTemplate.opsForSet().size(PREFIX + userId);
        return size != null ? size : 0;
    }
}
