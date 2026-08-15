package com.example.broken;

import com.example.broken.entity.BrokenCounterEntity;
import com.example.broken.repository.BrokenCounterRepository;
import com.example.broken.service.BrokenDbCounterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that BrokenDbCounterService loses updates under concurrent load.
 * 50 threads × 100 increments = should be 5000 but won't be due to lost updates.
 */
@SpringBootTest
class LostUpdateTest {

    private static final int THREAD_COUNT = 50;
    private static final int INCREMENTS_PER_THREAD = 100;
    private static final int EXPECTED_TOTAL = THREAD_COUNT * INCREMENTS_PER_THREAD;
    private static final String COUNTER_NAME = "db-test-counter";

    @Autowired
    private BrokenDbCounterService dbCounterService;

    @Autowired
    private BrokenCounterRepository counterRepository;

    @BeforeEach
    void setUp() {
        counterRepository.deleteAll();
        // Pre-create the counter to avoid race on initial insert
        counterRepository.saveAndFlush(new BrokenCounterEntity(COUNTER_NAME, 0L));
    }

    @RepeatedTest(3)
    @DisplayName("Lost update: concurrent DB increments lose updates (broken)")
    void concurrentDbIncrements_shouldLoseUpdates() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                        dbCounterService.increment(COUNTER_NAME);
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

        BrokenCounterEntity result = counterRepository.findByName(COUNTER_NAME).orElseThrow();
        long finalValue = result.getValue();

        System.out.println("Expected: " + EXPECTED_TOTAL + ", Got: " + finalValue +
                ", Lost: " + (EXPECTED_TOTAL - finalValue));
        assertThat(finalValue)
                .as("Broken DB counter should lose updates due to read-modify-write without locking")
                .isLessThan(EXPECTED_TOTAL);
    }
}
