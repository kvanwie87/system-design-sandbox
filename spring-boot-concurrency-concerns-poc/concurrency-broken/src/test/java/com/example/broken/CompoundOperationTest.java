package com.example.broken;

import com.example.broken.service.BrokenCompoundOperationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that compound operations on ConcurrentHashMap are NOT atomic.
 */
class CompoundOperationTest {

    @RepeatedTest(3)
    @DisplayName("Non-atomic compound op: get+add+put loses updates on ConcurrentHashMap (broken)")
    void incrementBy_shouldLoseUpdates() throws InterruptedException {
        BrokenCompoundOperationService service = new BrokenCompoundOperationService();
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
        System.out.println("[Compound Op] Expected: " + expectedTotal + ", Got: " + finalValue +
                ", Lost: " + (expectedTotal - finalValue));
        assertThat(finalValue)
                .as("get+add+put compound operation should lose updates")
                .isLessThan(expectedTotal);
    }

    @Test
    @DisplayName("Non-atomic compound op: containsKey+get causes NPE when entry removed concurrently")
    void getValueOrThrow_shouldThrowNPE() throws InterruptedException {
        BrokenCompoundOperationService service = new BrokenCompoundOperationService();
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

        System.out.println("[ContainsKey+Get] NPEs: " + npeCount.get() +
                ", Successes: " + successCount.get());
        assertThat(npeCount.get())
                .as("containsKey + get should produce NPE when entry is removed between calls")
                .isGreaterThan(0);
    }

    @RepeatedTest(3)
    @DisplayName("Non-atomic compound op: decrementIfPositive allows over-decrement (broken)")
    void decrementIfPositive_shouldAllowOverDecrement() throws InterruptedException {
        BrokenCompoundOperationService service = new BrokenCompoundOperationService();
        // Start at 5 — at most 5 decrements should succeed,
        // but the race allows multiple threads to see value > 0 and all decrement
        service.initCounter("limited", 5L);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successfulDecrements = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // Each thread does a single decrement-if-positive
                    long result = service.decrementIfPositive("limited");
                    // If the method returned a value less than the initial (meaning it decremented)
                    // we count it. With the bug, more than 5 will succeed.
                    if (result < 5) {
                        successfulDecrements.incrementAndGet();
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

        long finalValue = service.getValue("limited");
        System.out.println("[DecrementIfPositive] Successful decrements: " + successfulDecrements.get() +
                " (max allowed: 5), Final value: " + finalValue);

        // The bug: more threads succeed in decrementing than should be possible.
        // With correct atomic conditional decrement, at most 5 should succeed (value 5→4→3→2→1→0).
        // But without atomicity, many threads see value>0 simultaneously and all proceed.
        assertThat(successfulDecrements.get())
                .as("Non-atomic conditional decrement allows more decrements than the value permits")
                .isGreaterThan(5);
    }
}
