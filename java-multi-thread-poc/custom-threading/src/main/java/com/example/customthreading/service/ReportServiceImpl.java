package com.example.customthreading.service;

import com.example.customthreading.annotation.MyAsync;
import com.example.customthreading.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Async report generation service using our custom @MyAsync annotation.
 *
 * Behavior is identical to spring-threading's ReportService:
 * - generateReport() is intercepted by the proxy, which calls CompletableFuture.supplyAsync()
 *   on myAsyncExecutor, executing the method body on a pool thread.
 * - The task store (ConcurrentHashMap) tracks status: PENDING → COMPLETE.
 * - The controller doesn't await the future — it returns 202 with the task ID immediately.
 * - The client polls GET /reports/{id} to see when it's done.
 *
 * Note: registerTask() and getTaskStatus() are NOT annotated with @MyAsync —
 * they pass through the proxy and execute synchronously on the calling thread.
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final ConcurrentMap<String, TaskStatus> taskStore = new ConcurrentHashMap<>();

    @Override
    @MyAsync
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

    @Override
    public void registerTask(String taskId) {
        taskStore.put(taskId, TaskStatus.pending());
    }

    @Override
    public TaskStatus getTaskStatus(String taskId) {
        return taskStore.get(taskId);
    }
}
