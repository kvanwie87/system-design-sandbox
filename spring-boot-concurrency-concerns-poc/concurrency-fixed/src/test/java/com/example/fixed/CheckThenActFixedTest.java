package com.example.fixed;

import com.example.fixed.service.FixedCheckThenActService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that FixedCheckThenActService initializes each counter exactly once,
 * regardless of how many threads try to get-or-create simultaneously.
 */
class CheckThenActFixedTest {

    private static final int THREAD_COUNT = 50;

    @RepeatedTest(3)
    @DisplayName("Fixed: computeIfAbsent initializes counter exactly once")
    void concurrentGetOrCreate_shouldInitializeExactlyOnce() throws InterruptedException {
        FixedCheckThenActService service = new FixedCheckThenActService();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    service.getOrCreate("shared-counter", 100L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        int inits = service.getInitializationCount();
        System.out.println("Initialization count (should be 1): " + inits);

        assertThat(inits)
                .as("computeIfAbsent should initialize the counter exactly once")
                .isEqualTo(1);
    }
}
