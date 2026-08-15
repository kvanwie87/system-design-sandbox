package com.example.fixed;

import com.example.fixed.service.FixedCompoundOperationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that atomic compound operations (merge, computeIfPresent) are correct
 * under concurrent access.
 */
class CompoundOperationFixedTest {

    @RepeatedTest(3)
    @DisplayName("Fixed: merge() produces exact count (no lost updates)")
    void incrementBy_shouldProduceExactCount() throws InterruptedException {
        FixedCompoundOperationService service = new FixedCompoundOperationService();
        service.initCounter("counter", 0L);

        int threadCount = 50;
        int incrementsPerThread = 100;
        int expectedTotal = threadCount * incrementsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < incrementsPerThread; j++) {
                        service.incrementBy("counter", 1);
                    }
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

        long finalValue = service.getValue("counter");
        System.out.println("[merge()] Expected: " + expectedTotal + ", Got: " + finalValue);
        assertThat(finalValue)
                .as("merge() should produce exact count with no lost updates")
                .isEqualTo(expectedTotal);
    }

    @Test
    @DisplayName("Fixed: get() with null check avoids NPE when entry removed concurrently")
    void getValueOrThrow_shouldNotNPE() throws InterruptedException {
        FixedCompoundOperationService service = new FixedCompoundOperationService();
        service.initCounter("volatile-counter", 42L);

        int threadCount = 10;
        int iterations = 1000;
        AtomicInteger npeCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount + 1);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount + 1);

        // One thread continuously removes and re-adds the entry
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < iterations; i++) {
                    service.remove("volatile-counter");
                    service.initCounter("volatile-counter", 42L);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        // Other threads continuously try to read
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations; j++) {
                        try {
                            service.getValueOrThrow("volatile-counter");
                            successCount.incrementAndGet();
                        } catch (NullPointerException e) {
                            npeCount.incrementAndGet();
                        } catch (IllegalArgumentException e) {
                            // Expected when entry is genuinely absent
                            successCount.incrementAndGet();
                        }
                    }
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

        System.out.println("[Single get()] NPEs: " + npeCount.get() +
                ", Successes: " + successCount.get());
        assertThat(npeCount.get())
                .as("Single get() with null check should never produce NPE")
                .isZero();
    }

    @RepeatedTest(3)
    @DisplayName("Fixed: computeIfPresent() never goes below zero")
    void decrementIfPositive_shouldNeverGoBelowZero() throws InterruptedException {
        FixedCompoundOperationService service = new FixedCompoundOperationService();
        service.initCounter("limited", 5L);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    service.decrementIfPositive("limited");
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

        long finalValue = service.getValue("limited");
        System.out.println("[computeIfPresent()] Final value (should be 0): " + finalValue);

        assertThat(finalValue)
                .as("computeIfPresent should never allow value to go below zero")
                .isEqualTo(0L);
    }
}
