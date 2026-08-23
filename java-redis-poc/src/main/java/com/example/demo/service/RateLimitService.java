package com.example.demo.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.dto.RateLimitInfo;

@Service
public class RateLimitService {

    private final int maxRequests;
    private final int windowSeconds;
    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate,
                            @Value("${rate-limit.max-requests:10}") int maxRequests,
                            @Value("${rate-limit.window-seconds:60}") int windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public RateLimitInfo checkRateLimit(String clientId) {
        String key = "ratelimit:" + clientId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        long currentCount = count != null ? count : 1;
        boolean allowed = currentCount <= maxRequests;
        int remaining = Math.max(0, (int) (maxRequests - currentCount));
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long retryAfter = ttl != null && ttl > 0 ? ttl : windowSeconds;
        return new RateLimitInfo(allowed, remaining, retryAfter);
    }
}
