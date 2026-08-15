package com.example.fixed;

import com.example.fixed.service.FixedInMemoryCounterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that FixedInMemoryCounterService correctly handles concurrent access.
 * 100 threads × 1000 increments = exactly 100,000 every time.
 */
class RaceConditionFixedTest {

    private static final int THREAD_COUNT = 100;
    private static final int INCREMENTS_PER_THREAD = 1000;
    private static final int EXPECTED_TOTAL = THREAD_COUNT * INCREMENTS_PER_THREAD;

    @RepeatedTest(3)
    @DisplayName("Fixed: concurrent increments produce exact count (no lost updates)")
    void concurrentIncrements_shouldProduceExactCount() throws InterruptedException {
        FixedInMemoryCounterService service = new FixedInMemoryCounterService();
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

        System.out.println("Expected: " + EXPECTED_TOTAL + ", Got: " + finalValue);
        assertThat(finalValue)
                .as("Fixed counter should produce exact count with no lost updates")
                .isEqualTo(EXPECTED_TOTAL);
    }

    @Test
    @DisplayName("Fixed: volatile flag is visible across threads")
    void volatileFlag_shouldBeVisibleAcrossThreads() throws InterruptedException {
        FixedInMemoryCounterService service = new FixedInMemoryCounterService();

        assertThat(service.isShutdownRequested()).isFalse();

        // Writer thread sets the flag
        Thread writer = new Thread(service::requestShutdown);
        writer.start();
        writer.join();

        // After join(), the volatile write is guaranteed to be visible
        assertThat(service.isShutdownRequested())
                .as("Volatile flag should be visible after writer thread completes")
                .isTrue();
    }
}
