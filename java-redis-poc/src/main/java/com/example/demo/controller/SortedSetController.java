package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.SortedSetService;

/**
 * Demonstrates Redis Sorted Set operations through a task scheduler/priority queue.
 * The score represents a Unix timestamp (when the task should execute).
 *
 * This differentiates from the Leaderboard (which uses scores for ranking).
 * Here, scores represent TIME — enabling queries like "what's due now?" and
 * "give me the next task to process." This is how BullMQ handles delayed jobs
 * and how many distributed schedulers work under the hood.
 *
 * Redis commands demonstrated: ZADD, ZRANGEBYSCORE, ZRANGE, ZPOPMIN, ZREM, ZCARD
 */
@RestController
@RequestMapping("/api/scheduler")
public class SortedSetController {

    private final SortedSetService sortedSetService;

    public SortedSetController(SortedSetService sortedSetService) {
        this.sortedSetService = sortedSetService;
    }

    /**
     * Schedule a task at a specific epoch timestamp.
     * Example: POST /api/scheduler/jobs?taskId=send-report&executeAt=1724500000
     */
    @PostMapping("/{queueName}")
    public ResponseEntity<Void> schedule(@PathVariable String queueName,
                                         @RequestParam String taskId,
                                         @RequestParam double executeAt) {
        sortedSetService.schedule(queueName, taskId, executeAt);
        return ResponseEntity.ok().build();
    }

    /**
     * Schedule a task with a delay in seconds from now.
     * Example: POST /api/scheduler/jobs/delay?taskId=cleanup&delaySeconds=300
     */
    @PostMapping("/{queueName}/delay")
    public ResponseEntity<Map<String, Object>> scheduleDelayed(@PathVariable String queueName,
                                                               @RequestParam String taskId,
                                                               @RequestParam long delaySeconds) {
        double executeAt = sortedSetService.scheduleWithDelay(queueName, taskId, delaySeconds);
        return ResponseEntity.ok(Map.of("taskId", taskId, "scheduledAt", executeAt));
    }

    /**
     * Get all tasks that are due now (score <= current time).
     * These are ready for processing.
     */
    @GetMapping("/{queueName}/due")
    public ResponseEntity<List<Map<String, Object>>> getDueTasks(@PathVariable String queueName) {
        return ResponseEntity.ok(sortedSetService.getDueTasks(queueName));
    }

    /**
     * Get all scheduled tasks (regardless of due time), ordered by scheduled time.
     */
    @GetMapping("/{queueName}")
    public ResponseEntity<List<Map<String, Object>>> getAllTasks(@PathVariable String queueName) {
        return ResponseEntity.ok(sortedSetService.getAllTasks(queueName));
    }

    /**
     * Pop the next due task (removes it from the queue and returns it).
     * Returns null body if no tasks are currently due.
     */
    @DeleteMapping("/{queueName}/next")
    public ResponseEntity<Map<String, Object>> popNextDue(@PathVariable String queueName) {
        Map<String, Object> task = sortedSetService.popNextDue(queueName);
        return ResponseEntity.ok(task);
    }

    /**
     * Cancel a scheduled task.
     */
    @DeleteMapping("/{queueName}/{taskId}")
    public ResponseEntity<Void> cancel(@PathVariable String queueName, @PathVariable String taskId) {
        sortedSetService.cancel(queueName, taskId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get the number of pending tasks in the schedule.
     */
    @GetMapping("/{queueName}/count")
    public ResponseEntity<Map<String, Long>> pendingCount(@PathVariable String queueName) {
        return ResponseEntity.ok(Map.of("pending", sortedSetService.pendingCount(queueName)));
    }
}
