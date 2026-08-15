package com.example.broken;

import com.example.broken.service.BrokenTransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that BrokenTransferService deadlocks when two threads perform
 * opposing transfers simultaneously (A→B and B→A).
 * 
 * The test uses a timeout to detect the deadlock: if the transfers don't
 * complete within 5 seconds, at least one thread is stuck in deadlock.
 */
class DeadlockTest {

    @Test
    @DisplayName("Deadlock: opposing transfers cause circular wait (broken)")
    void opposingTransfers_shouldDeadlock() throws InterruptedException {
        BrokenTransferService service = new BrokenTransferService();
        service.initCounter("A", 1000);
        service.initCounter("B", 1000);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        // Thread 1: repeatedly transfer A → B
        Future<?> future1 = executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 100; i++) {
                    service.transfer("A", "B", 1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Thread 2: repeatedly transfer B → A (opposite direction)
        Future<?> future2 = executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 100; i++) {
                    service.transfer("B", "A", 1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        startLatch.countDown();

        // Wait with a timeout — if deadlock occurs, futures won't complete
        boolean completed1 = false;
        boolean completed2 = false;
        try {
            future1.get(5, TimeUnit.SECONDS);
            completed1 = true;
        } catch (TimeoutException e) {
            // Expected: deadlock prevents completion
        } catch (ExecutionException e) {
            // Unexpected exception
        }

        try {
            future2.get(5, TimeUnit.SECONDS);
            completed2 = true;
        } catch (TimeoutException e) {
            // Expected: deadlock prevents completion
        } catch (ExecutionException e) {
            // Unexpected exception
        }

        executor.shutdownNow();

        boolean deadlockDetected = !completed1 || !completed2;
        System.out.println("Thread 1 completed: " + completed1 + ", Thread 2 completed: " + completed2);
        System.out.println("Deadlock detected: " + deadlockDetected);

        assertThat(deadlockDetected)
                .as("Opposing transfers with inconsistent lock ordering should deadlock")
                .isTrue();
    }
}
