package com.example.demo.service;

import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.exception.ResourceNotFoundException;

@Service
public class HashService {

    private final RedisTemplate<String, Object> redisTemplate;

    public HashService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void putField(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public Map<Object, Object> getAll(String key) {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            throw new ResourceNotFoundException("Hash not found: " + key);
        }
        return redisTemplate.opsForHash().entries(key);
    }

    public Object getField(String key, String field) {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            throw new ResourceNotFoundException("Hash not found: " + key);
        }
        Object value = redisTemplate.opsForHash().get(key, field);
        if (value == null) {
            throw new ResourceNotFoundException("Field not found: " + field + " in hash: " + key);
        }
        return value;
    }

    public void deleteField(String key, String field) {
        redisTemplate.opsForHash().delete(key, field);
    }
}
