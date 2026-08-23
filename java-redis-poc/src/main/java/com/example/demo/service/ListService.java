package com.example.demo.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ListService {

    private static final String PREFIX = "queue:";

    private final RedisTemplate<String, Object> redisTemplate;

    public ListService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Enqueue a task (RPUSH — adds to tail for FIFO processing).
     */
    public void enqueue(String queueName, Map<String, Object> task) {
        redisTemplate.opsForList().rightPush(PREFIX + queueName, task);
    }

    /**
     * Dequeue a task (LPOP — removes from head for FIFO processing).
     * Returns null if the queue is empty.
     */
    public Object dequeue(String queueName) {
        return redisTemplate.opsForList().leftPop(PREFIX + queueName);
    }

    /**
     * Peek at all pending tasks without removing them (LRANGE 0 -1).
     */
    public List<Object> peek(String queueName) {
        List<Object> result = redisTemplate.opsForList().range(PREFIX + queueName, 0, -1);
        return result != null ? result : Collections.emptyList();
    }

    /**
     * Get the current queue depth (LLEN).
     */
    public long length(String queueName) {
        Long size = redisTemplate.opsForList().size(PREFIX + queueName);
        return size != null ? size : 0;
    }
}
