package com.example.demo.service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.example.demo.exception.ResourceNotFoundException;

@Service
public class SortedSetService {

    private final RedisTemplate<String, Object> redisTemplate;

    public SortedSetService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void add(String key, String member, double score) {
        redisTemplate.opsForZSet().add(key, member, score);
    }

    public Set<Object> getAll(String key) {
        Set<Object> result = redisTemplate.opsForZSet().range(key, 0, -1);
        return result != null ? result : Collections.emptySet();
    }

    public Set<Object> getRange(String key, long start, long end) {
        Set<Object> result = redisTemplate.opsForZSet().range(key, start, end);
        return result != null ? result : Collections.emptySet();
    }

    public Double getScore(String key, String member) {
        Double score = redisTemplate.opsForZSet().score(key, member);
        if (score == null) {
            throw new ResourceNotFoundException("Member not found: " + member + " in sorted set: " + key);
        }
        return score;
    }

    public void remove(String key, String member) {
        redisTemplate.opsForZSet().remove(key, member);
    }
}
