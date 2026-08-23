package com.example.demo.service;

import java.util.Collections;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ListService {

    private final RedisTemplate<String, Object> redisTemplate;

    public ListService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void push(String key, String value, String direction) {
        if ("right".equalsIgnoreCase(direction)) {
            redisTemplate.opsForList().rightPush(key, value);
        } else {
            redisTemplate.opsForList().leftPush(key, value);
        }
    }

    public List<Object> getAll(String key) {
        List<Object> result = redisTemplate.opsForList().range(key, 0, -1);
        return result != null ? result : Collections.emptyList();
    }

    public Object pop(String key, String direction) {
        if ("right".equalsIgnoreCase(direction)) {
            return redisTemplate.opsForList().rightPop(key);
        } else {
            return redisTemplate.opsForList().leftPop(key);
        }
    }
}
