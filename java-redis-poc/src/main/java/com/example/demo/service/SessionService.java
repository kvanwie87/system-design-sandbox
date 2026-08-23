package com.example.demo.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.exception.ResourceNotFoundException;

@Service
public class SessionService {

    private static final String SESSION_PREFIX = "session:";

    private final RedisTemplate<String, Object> redisTemplate;

    public SessionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void putAttribute(String sessionId, String attributeKey, String attributeValue, Long ttlSeconds) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.opsForHash().put(key, attributeKey, attributeValue);
        if (ttlSeconds != null) {
            redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        }
    }

    public Map<Object, Object> getAttributes(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            throw new ResourceNotFoundException("Session not found: " + sessionId);
        }
        return redisTemplate.opsForHash().entries(key);
    }

    public void deleteSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.delete(key);
    }
}
