package com.example.fixed;

import com.example.fixed.service.RequestScopedContext;
import com.example.fixed.service.StatelessRequestService;
import com.example.fixed.service.StatelessRequestService.RequestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that the stateless and request-scoped fixes prevent shared field corruption.
 */
class SharedFieldFixedTest {

    private static final int ITERATIONS = 200;

    @RepeatedTest(3)
    @DisplayName("Fix #1 (Stateless): no shared fields, no corruption under concurrency")
    void statelessService_shouldNeverCorrupt() throws Exception {
        StatelessRequestService service = new StatelessRequestService();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger corruptions = new AtomicInteger(0);

        CyclicBarrier barrier = new CyclicBarrier(2);

        Future<?> userA = executor.submit(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                try {
                    barrier.await();
                } catch (Exception e) {
                    return;
                }
                RequestResult result = service.processRequest("UserA", List.of(10L, 20L));
                if (!"UserA".equals(result.user()) || result.total() != 30) {
                    corruptions.incrementAndGet();
                }
            }
        });

        Future<?> userB = executor.submit(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                try {
                    barrier.await();
                } catch (Exception e) {
                    return;
                }
                RequestResult result = service.processRequest("UserB", List.of(100L, 200L, 300L));
                if (!"UserB".equals(result.user()) || result.total() != 600) {
                    corruptions.incrementAndGet();
                }
            }
        });

        userA.get();
        userB.get();
        executor.shutdown();

        System.out.println("[Stateless] Corruptions: " + corruptions.get() + "/" + (ITERATIONS * 2));
        assertThat(corruptions.get())
                .as("Stateless service should have zero corruptions")
                .isZero();
    }

    @RepeatedTest(3)
    @DisplayName("Fix #2 (Request-scoped): each thread gets its own context, no corruption")
    void requestScopedContext_shouldNeverCorrupt() throws Exception {
        // Simulate request-scoping: each thread creates its own instance
        // (In production, Spring creates a new instance per HTTP request via the proxy)
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger corruptions = new AtomicInteger(0);

        CyclicBarrier barrier = new CyclicBarrier(2);

        Future<?> userA = executor.submit(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                try {
                    barrier.await();
                } catch (Exception e) {
                    return;
                }
                // Each request gets its own RequestScopedContext (simulating @RequestScope)
                RequestScopedContext context = new RequestScopedContext();
                context.beginRequest("UserA");
                context.addAmount(10);
                context.addAmount(20);
                RequestScopedContext.RequestResult result = context.finishRequest();

                if (!"UserA".equals(result.user()) || result.total() != 30) {
                    corruptions.incrementAndGet();
                }
            }
        });

        Future<?> userB = executor.submit(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                try {
                    barrier.await();
                } catch (Exception e) {
                    return;
                }
                RequestScopedContext context = new RequestScopedContext();
                context.beginRequest("UserB");
                context.addAmount(100);
                context.addAmount(200);
                context.addAmount(300);
                RequestScopedContext.RequestResult result = context.finishRequest();

                if (!"UserB".equals(result.user()) || result.total() != 600) {
                    corruptions.incrementAndGet();
                }
            }
        });

        userA.get();
        userB.get();
        executor.shutdown();

        System.out.println("[RequestScoped] Corruptions: " + corruptions.get() + "/" + (ITERATIONS * 2));
        assertThat(corruptions.get())
                .as("Request-scoped context should have zero corruptions (one instance per request)")
                .isZero();
    }

    @Test
    @DisplayName("Fix #2: Verify request-scoped context isolates state")
    void requestScopedContext_shouldIsolateState() {
        // Simulating two separate "requests" — each gets its own context
        RequestScopedContext request1 = new RequestScopedContext();
        RequestScopedContext request2 = new RequestScopedContext();

        request1.beginRequest("Alice");
        request1.addAmount(50);

        request2.beginRequest("Bob");
        request2.addAmount(999);

        // Each context is independent
        RequestScopedContext.RequestResult result1 = request1.finishRequest();
        RequestScopedContext.RequestResult result2 = request2.finishRequest();

        assertThat(result1.user()).isEqualTo("Alice");
        assertThat(result1.total()).isEqualTo(50);
        assertThat(result2.user()).isEqualTo("Bob");
        assertThat(result2.total()).isEqualTo(999);
    }
}
