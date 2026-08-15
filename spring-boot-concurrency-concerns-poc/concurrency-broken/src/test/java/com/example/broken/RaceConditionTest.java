package com.example.broken;

import com.example.broken.service.BrokenInMemoryCounterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that BrokenInMemoryCounterService loses updates under concurrent load.
 * 100 threads × 1000 increments = should be 100,000 but won't be due to race conditions.
 */
class RaceConditionTest {

    private static final int THREAD_COUNT = 100;
    private static final int INCREMENTS_PER_THREAD = 1000;
    private static final int EXPECTED_TOTAL = THREAD_COUNT * INCREMENTS_PER_THREAD;

    @RepeatedTest(3)
    @DisplayName("Race condition: concurrent increments lose updates (broken)")
    void concurrentIncrements_shouldLoseUpdates() throws InterruptedException {
        BrokenInMemoryCounterService service = new BrokenInMemoryCounterService();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start at the same time
                    for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                        service.increment("test-counter");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all threads simultaneously
        doneLatch.await();
        executor.shutdown();

        long finalValue = service.getValue("test-counter");

        // The broken service should lose updates due to race conditions
        // Final value will be less than expected (almost certainly)
        System.out.println("Expected: " + EXPECTED_TOTAL + ", Got: " + finalValue +
                ", Lost: " + (EXPECTED_TOTAL - finalValue));
        assertThat(finalValue)
                .as("Broken counter should lose updates due to race condition")
                .isLessThan(EXPECTED_TOTAL);
    }

    @Test
    @DisplayName("Visibility issue: non-volatile flag may not be seen by reader thread")
    void visibilityIssue_nonVolatileFlag() throws InterruptedException {
        BrokenInMemoryCounterService service = new BrokenInMemoryCounterService();

        // This test demonstrates the concept — in practice the JVM *may* propagate
        // the write, but without volatile there's no guarantee.
        // We verify the API exists and the flag works in single-threaded context.
        assertThat(service.isShutdownRequested()).isFalse();
        service.requestShutdown();
        assertThat(service.isShutdownRequested()).isTrue();

        // Note: Proving visibility bugs deterministically is very difficult because
        // the JVM may or may not optimize away the read. The real proof is in the
        // code review: without volatile, the JIT compiler is free to cache the value
        // in a register and never re-read from main memory.
    }
}
