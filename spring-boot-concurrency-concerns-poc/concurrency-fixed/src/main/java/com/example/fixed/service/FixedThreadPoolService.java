package com.example.fixed.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * Fixed service demonstrating the bulkhead pattern to prevent thread starvation.
 * 
 * FIX: Separate thread pools for different task categories.
 * Slow tasks cannot starve fast tasks because they use independent pools.
 * 
 * This is the "bulkhead" pattern — named after ship compartments that prevent
 * a single breach from sinking the whole vessel.
 * 
 * Additional improvements:
 * - Bounded queues with rejection policy to fail fast rather than queue indefinitely
 * - Separate pools sized appropriately for their workload characteristics
 */
@Service
public class FixedThreadPoolService {

    // Dedicated pool for slow/background tasks
    private final ExecutorService slowTaskPool = new ThreadPoolExecutor(
            2,                          // core threads
            2,                          // max threads
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(10),
            new ThreadPoolExecutor.CallerRunsPolicy() // backpressure: caller executes if full
    );

    // Dedicated pool for fast/critical tasks — isolated from slow tasks
    private final ExecutorService fastTaskPool = new ThreadPoolExecutor(
            4,                          // more threads for fast tasks
            4,                          // max threads
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(100)
    );

    /**
     * Submits a slow task to the SLOW pool — cannot starve fast tasks.
     */
    public Future<String> submitSlowTask(String taskId, long durationMs) {
        return slowTaskPool.submit(() -> {
            Thread.sleep(durationMs);
            return "slow-" + taskId + "-done";
        });
    }

    /**
     * Submits a fast task to the FAST pool — isolated from slow tasks.
     * FIX: Fast tasks always have threads available regardless of slow task load.
     */
    public Future<String> submitFastTask(String taskId) {
        return fastTaskPool.submit(() -> "fast-" + taskId + "-done");
    }

    public void shutdown() {
        slowTaskPool.shutdownNow();
        fastTaskPool.shutdownNow();
    }
}
