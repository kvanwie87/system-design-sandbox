package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.ListService;

/**
 * Demonstrates Redis List operations through a task queue use case.
 * Producers enqueue tasks (RPUSH), consumers dequeue them (LPOP) — FIFO order.
 *
 * This uses the same underlying Redis list commands (RPUSH/LPOP) that
 * Sidekiq (Ruby) and BullMQ (Node.js) are built on for job processing.
 * Production frameworks add blocking pops (BRPOP) and reliability tracking,
 * but the fundamental data structure pattern is identical.
 */
@RestController
@RequestMapping("/api/queues")
public class ListController {

    private final ListService listService;

    public ListController(ListService listService) {
        this.listService = listService;
    }

    /**
     * Enqueue a task (producer).
     * Example: POST /api/queues/emails {"type":"welcome","to":"alice@example.com"}
     */
    @PostMapping("/{queueName}")
    public ResponseEntity<Void> enqueue(@PathVariable String queueName,
                                        @RequestBody Map<String, Object> task) {
        listService.enqueue(queueName, task);
        return ResponseEntity.ok().build();
    }

    /**
     * Dequeue the next task (consumer). Returns the task or null if empty.
     */
    @DeleteMapping("/{queueName}/next")
    public ResponseEntity<Object> dequeue(@PathVariable String queueName) {
        Object task = listService.dequeue(queueName);
        return ResponseEntity.ok(task);
    }

    /**
     * Peek at all pending tasks without consuming them.
     */
    @GetMapping("/{queueName}")
    public ResponseEntity<List<Object>> peek(@PathVariable String queueName) {
        return ResponseEntity.ok(listService.peek(queueName));
    }

    /**
     * Get queue depth (number of pending tasks).
     */
    @GetMapping("/{queueName}/length")
    public ResponseEntity<Map<String, Long>> length(@PathVariable String queueName) {
        return ResponseEntity.ok(Map.of("length", listService.length(queueName)));
    }
}
