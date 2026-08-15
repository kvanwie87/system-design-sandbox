package com.example.springthreading.service;

import com.example.springthreading.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Demonstrates the "async with result" pattern using @Async + CompletableFuture.
 *
 * How it works under the hood:
 * 1. The proxy intercepts generateReport() and submits it to the TaskExecutor.
 * 2. Since the return type is CompletableFuture, Spring's proxy wraps the actual invocation
 *    in a Callable and returns a CompletableFuture that completes when the method finishes.
 * 3. Meanwhile, the caller (controller) doesn't await the future — it just registers the task
 *    as PENDING and returns a 202 with the task ID.
 * 4. The client polls GET /reports/{id} to check when status flips to COMPLETE.
 *
 * The in-memory ConcurrentHashMap acts as a simple task store. In production you'd use
 * Redis, a database, or a message broker for distributed state.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ConcurrentMap<String, TaskStatus> taskStore = new ConcurrentHashMap<>();

    @Async
    public CompletableFuture<String> generateReport(String taskId) {
        log.info("Starting report generation for taskId={} on thread: {}", taskId, Thread.currentThread().getName());
        try {
            // Simulate expensive computation (e.g., aggregating data, generating PDF)
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String result = "Report-" + taskId + " generated at " + System.currentTimeMillis();
        taskStore.put(taskId, TaskStatus.complete(result));
        log.info("Report generation complete for taskId={}", taskId);
        return CompletableFuture.completedFuture(result);
    }

    /** Register a task as PENDING before submitting the async work. */
    public void registerTask(String taskId) {
        taskStore.put(taskId, TaskStatus.pending());
    }

    /** Look up current task status. Returns null if taskId is unknown. */
    public TaskStatus getTaskStatus(String taskId) {
        return taskStore.get(taskId);
    }
}
