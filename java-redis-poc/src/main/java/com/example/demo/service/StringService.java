package com.example.demo.service;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.exception.ResourceNotFoundException;

@Service
public class StringService {

    private static final String PREFIX = "flag:";

    private final RedisTemplate<String, Object> redisTemplate;

    public StringService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Set a feature flag value (SET), optionally with TTL (SET EX).
     */
    public void setFlag(String flagName, String value, Long ttlSeconds) {
        String key = PREFIX + flagName;
        if (ttlSeconds != null && ttlSeconds > 0) {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    /**
     * Create a flag only if it doesn't exist (SETNX).
     * Returns true if created, false if it already existed.
     */
    public boolean createIfAbsent(String flagName, String value, Long ttlSeconds) {
        String key = PREFIX + flagName;
        Boolean created = redisTemplate.opsForValue().setIfAbsent(key, value);
        if (Boolean.TRUE.equals(created) && ttlSeconds != null && ttlSeconds > 0) {
            redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        }
        return Boolean.TRUE.equals(created);
    }

    /**
     * Get a flag's value along with TTL info.
     */
    public Map<String, Object> getFlag(String flagName) {
        String key = PREFIX + flagName;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            throw new ResourceNotFoundException("Flag not found: " + flagName);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("flagName", flagName);
        result.put("value", value.toString());

        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            result.put("ttlSeconds", ttl);
        } else {
            result.put("ttlSeconds", null);
        }
        return result;
    }

    /**
     * Toggle a boolean flag between "true" and "false".
     * If the flag doesn't exist, creates it with "true".
     */
    public Map<String, Object> toggleFlag(String flagName) {
        String key = PREFIX + flagName;
        Object current = redisTemplate.opsForValue().get(key);
        String newValue;
        if (current == null || "false".equalsIgnoreCase(current.toString())) {
            newValue = "true";
        } else {
            newValue = "false";
        }
        redisTemplate.opsForValue().set(key, newValue);
        return Map.of("flagName", flagName, "value", newValue);
    }

    /**
     * Delete a feature flag (DEL).
     */
    public void deleteFlag(String flagName) {
        redisTemplate.delete(PREFIX + flagName);
    }

    /**
     * List all flag names using SCAN with the prefix pattern.
     */
    public Set<String> listFlags() {
        Set<String> keys = redisTemplate.keys(PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptySet();
        }
        return keys.stream()
                .map(k -> k.substring(PREFIX.length()))
                .collect(Collectors.toSet());
    }
}
