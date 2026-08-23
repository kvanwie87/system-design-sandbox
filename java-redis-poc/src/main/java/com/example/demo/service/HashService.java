package com.example.demo.service;

import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.exception.ResourceNotFoundException;

@Service
public class HashService {

    private static final String PREFIX = "user:";

    private final RedisTemplate<String, Object> redisTemplate;

    public HashService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveProfile(String userId, Map<String, String> fields) {
        String key = PREFIX + userId;
        fields.forEach((field, value) -> redisTemplate.opsForHash().put(key, field, value));
    }

    public void updateField(String userId, String field, String value) {
        redisTemplate.opsForHash().put(PREFIX + userId, field, value);
    }

    public Map<Object, Object> getProfile(String userId) {
        String key = PREFIX + userId;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return redisTemplate.opsForHash().entries(key);
    }

    public Object getField(String userId, String field) {
        String key = PREFIX + userId;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        Object value = redisTemplate.opsForHash().get(key, field);
        if (value == null) {
            throw new ResourceNotFoundException("Field not found: " + field + " for user: " + userId);
        }
        return value;
    }

    public void deleteField(String userId, String field) {
        redisTemplate.opsForHash().delete(PREFIX + userId, field);
    }

    public void deleteProfile(String userId) {
        redisTemplate.delete(PREFIX + userId);
    }
}
