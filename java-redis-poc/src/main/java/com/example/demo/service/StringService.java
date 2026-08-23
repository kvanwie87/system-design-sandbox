package com.example.demo.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.exception.ResourceNotFoundException;

@Service
public class StringService {

    private final RedisTemplate<String, Object> redisTemplate;

    public StringService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            throw new ResourceNotFoundException("Key not found: " + key);
        }
        return value.toString();
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
