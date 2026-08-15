package com.example.broken;

import com.example.broken.service.BrokenSharedFieldService;
import com.example.broken.service.BrokenSharedFieldService.RequestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that BrokenSharedFieldService corrupts state when accessed concurrently.
 * Multiple threads process requests for different users on the SAME singleton instance.
 * Threads will see each other's state (wrong user, wrong totals, exceptions).
 */
class SharedFieldTest {

    private static final int THREAD_COUNT = 4;
    private static final int ITERATIONS_PER_THREAD = 500;

    @Test
    @DisplayName("Shared fields: singleton state bleeds between concurrent requests (broken)")
    void sharedFields_shouldBleedBetweenRequests() throws Exception {
        BrokenSharedFieldService service = new BrokenSharedFieldService();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger corruptions = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        for (int t = 0; t < THREAD_COUNT; t++) {
            final String user = "User" + t;
            final long perAdd = (t + 1) * 10L; // 10, 20, 30, 40
            final long expectedTotal = perAdd * 3; // each request adds 3 times

            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                        try {
                            service.beginRequest(user);
                            service.addAmount(perAdd);
                            service.addAmount(perAdd);
                            service.addAmount(perAdd);
                            RequestResult result = service.finishRequest();

                            if (!user.equals(result.user()) || result.total() != expectedTotal) {
                                corruptions.incrementAndGet();
                            }
                        } catch (Exception e) {
                            // NPE, ConcurrentModificationException — proof of corruption
                            corruptions.incrementAndGet();
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

        int totalOps = THREAD_COUNT * ITERATIONS_PER_THREAD;
        System.out.println("Shared field corruptions detected: " + corruptions.get() + "/" + totalOps);
        assertThat(corruptions.get())
                .as("Shared singleton fields should corrupt under concurrent access")
                .isGreaterThan(0);
    }
}
