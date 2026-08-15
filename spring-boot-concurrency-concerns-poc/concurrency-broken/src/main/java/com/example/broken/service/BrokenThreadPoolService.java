package com.example.broken.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * Deliberately broken service demonstrating thread starvation.
 * 
 * BUG: A single small thread pool handles BOTH slow (long-running) and fast tasks.
 * When slow tasks fill the pool, fast tasks are starved — they sit in the queue
 * indefinitely waiting for a thread to become available.
 * 
 * In real apps this manifests as:
 * - Health checks timing out because the shared pool is exhausted
 * - Simple queries taking seconds because the pool is full of slow reports
 * - Cascading failures when downstream timeouts hold threads hostage
 */
@Service
public class BrokenThreadPoolService {

    // BUG: Single tiny pool shared by all task types
    private final ExecutorService sharedPool = new ThreadPoolExecutor(
            2,                          // core threads
            2,                          // max threads (no elastic growth)
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(100) // tasks queue up when pool is full
    );

    /**
     * Submits a slow task that holds a thread for a long time.
     */
    public Future<String> submitSlowTask(String taskId, long durationMs) {
        return sharedPool.submit(() -> {
            Thread.sleep(durationMs);
            return "slow-" + taskId + "-done";
        });
    }

    /**
     * Submits a fast task that should complete nearly instantly.
     * BUG: This shares the pool with slow tasks, so it may wait indefinitely.
     */
    public Future<String> submitFastTask(String taskId) {
        return sharedPool.submit(() -> "fast-" + taskId + "-done");
    }

    public void shutdown() {
        sharedPool.shutdownNow();
    }
}
