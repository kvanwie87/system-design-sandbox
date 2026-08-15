package com.example.fixed;

import com.example.fixed.service.FixedTransferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that FixedTransferService does NOT deadlock when two threads perform
 * opposing transfers simultaneously (A→B and B→A).
 * 
 * Both threads complete within timeout, and balances remain consistent (zero-sum).
 */
class DeadlockFixedTest {

    @Test
    @DisplayName("Fixed: opposing transfers complete without deadlock (ordered locking)")
    void opposingTransfers_shouldNotDeadlock() throws Exception {
        FixedTransferService service = new FixedTransferService();
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

        // Both should complete within timeout — no deadlock
        future1.get(10, TimeUnit.SECONDS);
        future2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        long balanceA = service.getBalance("A");
        long balanceB = service.getBalance("B");
        long totalBalance = balanceA + balanceB;

        System.out.println("Balance A: " + balanceA + ", Balance B: " + balanceB +
                ", Total: " + totalBalance);

        // Both threads did 100 transfers of 1 in opposite directions — net effect is zero
        assertThat(totalBalance)
                .as("Total balance should be conserved (zero-sum transfers)")
                .isEqualTo(2000L);

        // Each counter should still equal 1000 (100 transfers each way cancel out)
        assertThat(balanceA)
                .as("Balance A should be 1000 (equal opposing transfers)")
                .isEqualTo(1000L);
        assertThat(balanceB)
                .as("Balance B should be 1000 (equal opposing transfers)")
                .isEqualTo(1000L);
    }

    @Test
    @DisplayName("Fixed: asymmetric transfers maintain conservation")
    void asymmetricTransfers_shouldMaintainConservation() throws Exception {
        FixedTransferService service = new FixedTransferService();
        service.initCounter("X", 5000);
        service.initCounter("Y", 5000);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // Half the threads transfer X→Y, other half transfer Y→X
        for (int i = 0; i < threadCount; i++) {
            final int threadIdx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 200; j++) {
                        if (threadIdx % 2 == 0) {
                            service.transfer("X", "Y", 1);
                        } else {
                            service.transfer("Y", "X", 1);
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
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed)
                .as("All transfers should complete without deadlock within timeout")
                .isTrue();

        long totalBalance = service.getBalance("X") + service.getBalance("Y");
        System.out.println("Balance X: " + service.getBalance("X") +
                ", Balance Y: " + service.getBalance("Y") + ", Total: " + totalBalance);

        assertThat(totalBalance)
                .as("Total balance must be conserved across all transfers")
                .isEqualTo(10000L);
    }
}
