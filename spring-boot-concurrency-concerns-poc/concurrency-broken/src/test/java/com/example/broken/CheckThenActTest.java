package com.example.broken;

import com.example.broken.service.BrokenCheckThenActService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that BrokenCheckThenActService initializes the same counter multiple times
 * when concurrent threads all try to "get or create" simultaneously.
 */
class CheckThenActTest {

    private static final int THREAD_COUNT = 50;

    @Test
    @DisplayName("Check-then-act: multiple threads initialize the same counter (broken)")
    void concurrentGetOrCreate_shouldDuplicateInitialization() throws InterruptedException {
        BrokenCheckThenActService service = new BrokenCheckThenActService();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        // All threads try to get-or-create the SAME counter simultaneously
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
                .as("Broken check-then-act should initialize the same counter multiple times")
                .isGreaterThan(1);
    }
}
