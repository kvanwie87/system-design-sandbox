package com.example.fixed;

import com.example.fixed.service.FixedThreadPoolService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that FixedThreadPoolService (bulkhead pattern) prevents fast task starvation.
 * Slow tasks run in their own pool, so fast tasks always have threads available.
 */
class ThreadStarvationFixedTest {

    private final FixedThreadPoolService service = new FixedThreadPoolService();

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    @DisplayName("Fixed (bulkhead): fast tasks execute immediately despite slow task load")
    void fastTasks_shouldNotBeStarvedBySlowTasks() throws Exception {
        // Fill the slow-task pool with long-running work
        Future<String> slow1 = service.submitSlowTask("1", 5000);
        Future<String> slow2 = service.submitSlowTask("2", 5000);

        // Give slow tasks time to start
        Thread.sleep(100);

        // Submit fast tasks — they should complete instantly because they use a separate pool
        Future<String> fast1 = service.submitFastTask("quick1");
        Future<String> fast2 = service.submitFastTask("quick2");
        Future<String> fast3 = service.submitFastTask("quick3");

        // Fast tasks should complete well within 1 second
        String result1 = fast1.get(1, TimeUnit.SECONDS);
        String result2 = fast2.get(1, TimeUnit.SECONDS);
        String result3 = fast3.get(1, TimeUnit.SECONDS);

        System.out.println("Fast task results: " + result1 + ", " + result2 + ", " + result3);

        assertThat(result1).isEqualTo("fast-quick1-done");
        assertThat(result2).isEqualTo("fast-quick2-done");
        assertThat(result3).isEqualTo("fast-quick3-done");
    }

    @Test
    @DisplayName("Fixed (bulkhead): slow tasks still complete eventually")
    void slowTasks_shouldStillComplete() throws Exception {
        Future<String> slow = service.submitSlowTask("test", 200); // only 200ms

        String result = slow.get(2, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("slow-test-done");
    }
}
