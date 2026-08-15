package com.example.fixed;

import com.example.common.entity.CounterEntity;
import com.example.common.repository.CounterRepository;
import com.example.fixed.service.FixedDbCounterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that FixedDbCounterService correctly handles concurrent access using locking.
 * 50 threads × 100 increments = exactly 5000 every time.
 */
@SpringBootTest
class LostUpdateFixedTest {

    private static final int THREAD_COUNT = 50;
    private static final int INCREMENTS_PER_THREAD = 100;
    private static final int EXPECTED_TOTAL = THREAD_COUNT * INCREMENTS_PER_THREAD;

    @Autowired
    private FixedDbCounterService dbCounterService;

    @Autowired
    private CounterRepository counterRepository;

    @BeforeEach
    void setUp() {
        counterRepository.deleteAll();
    }

    @Test
    @DisplayName("Fixed (pessimistic locking): concurrent DB increments produce exact count")
    void concurrentDbIncrements_pessimistic_shouldProduceExactCount() throws InterruptedException {
        String counterName = "pessimistic-counter";
        counterRepository.saveAndFlush(new CounterEntity(counterName, 0L));

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                        dbCounterService.incrementPessimistic(counterName);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        CounterEntity result = counterRepository.findByName(counterName).orElseThrow();
        long finalValue = result.getValue();

        System.out.println("[Pessimistic] Expected: " + EXPECTED_TOTAL + ", Got: " + finalValue +
                ", Errors: " + errors.get());
        assertThat(errors.get()).isZero();
        assertThat(finalValue)
                .as("Pessimistic locking should produce exact count with no lost updates")
                .isEqualTo(EXPECTED_TOTAL);
    }

    @Test
    @DisplayName("Fixed (optimistic locking with retry): concurrent DB increments produce exact count")
    void concurrentDbIncrements_optimistic_shouldProduceExactCount() throws InterruptedException {
        String counterName = "optimistic-counter";
        counterRepository.saveAndFlush(new CounterEntity(counterName, 0L));

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                        dbCounterService.incrementOptimistic(counterName);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        CounterEntity result = counterRepository.findByName(counterName).orElseThrow();
        long finalValue = result.getValue();

        System.out.println("[Optimistic] Expected: " + EXPECTED_TOTAL + ", Got: " + finalValue +
                ", Errors: " + errors.get());
        assertThat(errors.get()).isZero();
        assertThat(finalValue)
                .as("Optimistic locking with retry should produce exact count with no lost updates")
                .isEqualTo(EXPECTED_TOTAL);
    }
}
