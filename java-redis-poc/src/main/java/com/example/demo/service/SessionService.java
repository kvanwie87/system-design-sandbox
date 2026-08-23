package com.example.demo.service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.exception.ResourceNotFoundException;

@Service
public class SessionService {

    private static final String SESSION_PREFIX = "session:";
    private static final long DEFAULT_TTL_SECONDS = 1800; // 30 minutes

    private final RedisTemplate<String, Object> redisTemplate;

    public SessionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Creates a new session with a generated UUID and stores initial attributes.
     * Sets a default TTL of 30 minutes.
     */
    public String createSession(Map<String, String> attributes) {
        String sessionId = UUID.randomUUID().toString();
        String key = SESSION_PREFIX + sessionId;
        if (attributes != null && !attributes.isEmpty()) {
            attributes.forEach((field, value) -> redisTemplate.opsForHash().put(key, field, value));
        }
        redisTemplate.expire(key, Duration.ofSeconds(DEFAULT_TTL_SECONDS));
        return sessionId;
    }

    /**
     * Gets all session attributes. Refreshes the TTL on access (sliding expiration).
     */
    public Map<Object, Object> getSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            throw new ResourceNotFoundException("Session not found: " + sessionId);
        }
        // Sliding expiration: refresh TTL on each access
        redisTemplate.expire(key, Duration.ofSeconds(DEFAULT_TTL_SECONDS));
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Updates or adds attributes to an existing session. Refreshes TTL.
     */
    public void updateSession(String sessionId, Map<String, String> attributes) {
        String key = SESSION_PREFIX + sessionId;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            throw new ResourceNotFoundException("Session not found: " + sessionId);
        }
        attributes.forEach((field, value) -> redisTemplate.opsForHash().put(key, field, value));
        redisTemplate.expire(key, Duration.ofSeconds(DEFAULT_TTL_SECONDS));
    }

    /**
     * Destroys a session (logout).
     */
    public void destroySession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            throw new ResourceNotFoundException("Session not found: " + sessionId);
        }
        redisTemplate.delete(key);
    }
}
