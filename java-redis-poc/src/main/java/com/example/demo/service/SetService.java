package com.example.demo.service;

import java.util.Collections;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SetService {

    private final RedisTemplate<String, Object> redisTemplate;

    public SetService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void add(String key, String member) {
        redisTemplate.opsForSet().add(key, member);
    }

    public Set<Object> getAll(String key) {
        Set<Object> result = redisTemplate.opsForSet().members(key);
        return result != null ? result : Collections.emptySet();
    }

    public boolean isMember(String key, String member) {
        Boolean result = redisTemplate.opsForSet().isMember(key, member);
        return Boolean.TRUE.equals(result);
    }

    public void remove(String key, String member) {
        redisTemplate.opsForSet().remove(key, member);
    }
}
