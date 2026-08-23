package com.example.demo.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
public class SortedSetService {

    private static final String PREFIX = "scheduler:";

    private final RedisTemplate<String, Object> redisTemplate;

    public SortedSetService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Schedule a task at a specific Unix timestamp (ZADD).
     * Score = epoch seconds when the task should execute.
     */
    public void schedule(String queueName, String taskId, double executeAt) {
        redisTemplate.opsForZSet().add(PREFIX + queueName, taskId, executeAt);
    }

    /**
     * Schedule a task with a delay from now.
     */
    public double scheduleWithDelay(String queueName, String taskId, long delaySeconds) {
        double executeAt = Instant.now().getEpochSecond() + delaySeconds;
        redisTemplate.opsForZSet().add(PREFIX + queueName, taskId, executeAt);
        return executeAt;
    }

    /**
     * Get all tasks due now or earlier (ZRANGEBYSCORE -inf to now).
     * These are ready for processing.
     */
    public List<Map<String, Object>> getDueTasks(String queueName) {
        double now = Instant.now().getEpochSecond();
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().rangeByScoreWithScores(PREFIX + queueName, 0, now);
        return tuplesToList(tuples);
    }

    /**
     * Get all scheduled tasks regardless of due time (ZRANGE with scores).
     */
    public List<Map<String, Object>> getAllTasks(String queueName) {
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().rangeWithScores(PREFIX + queueName, 0, -1);
        return tuplesToList(tuples);
    }

    /**
     * Pop the next due task — the one with the lowest score that's <= now (ZPOPMIN equivalent).
     * Returns null if no tasks are due.
     */
    public Map<String, Object> popNextDue(String queueName) {
        double now = Instant.now().getEpochSecond();
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().rangeByScoreWithScores(PREFIX + queueName, 0, now, 0, 1);
        if (tuples == null || tuples.isEmpty()) {
            return null;
        }
        ZSetOperations.TypedTuple<Object> next = tuples.iterator().next();
        // Remove it atomically
        redisTemplate.opsForZSet().remove(PREFIX + queueName, next.getValue());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", next.getValue() != null ? next.getValue().toString() : "");
        result.put("scheduledAt", next.getScore());
        return result;
    }

    /**
     * Cancel a scheduled task (ZREM).
     */
    public void cancel(String queueName, String taskId) {
        redisTemplate.opsForZSet().remove(PREFIX + queueName, taskId);
    }

    /**
     * Get the number of pending tasks (ZCARD).
     */
    public long pendingCount(String queueName) {
        Long size = redisTemplate.opsForZSet().size(PREFIX + queueName);
        return size != null ? size : 0;
    }

    private List<Map<String, Object>> tuplesToList(Set<ZSetOperations.TypedTuple<Object>> tuples) {
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("taskId", tuple.getValue() != null ? tuple.getValue().toString() : "");
            entry.put("scheduledAt", tuple.getScore());
            result.add(entry);
        }
        return result;
    }
}
