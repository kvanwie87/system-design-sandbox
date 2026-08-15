package com.example.broken;

import com.example.broken.service.BrokenThreadPoolService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that BrokenThreadPoolService starves fast tasks when slow tasks fill the pool.
 * The pool has only 2 threads — once both are occupied by slow tasks,
 * fast tasks sit in the queue unable to execute.
 */
class ThreadStarvationTest {

    private final BrokenThreadPoolService service = new BrokenThreadPoolService();

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    @DisplayName("Thread starvation: fast tasks starved by slow tasks in shared pool (broken)")
    void fastTasks_shouldBeStarvedBySlowTasks() throws Exception {
        // Fill the pool with slow tasks (pool size = 2)
        Future<String> slow1 = service.submitSlowTask("1", 5000); // holds thread for 5s
        Future<String> slow2 = service.submitSlowTask("2", 5000); // holds thread for 5s

        // Give slow tasks time to start and occupy both threads
        Thread.sleep(100);

        // Submit a fast task — it should complete instantly, but can't get a thread
        Future<String> fast = service.submitFastTask("quick");

        // Try to get the fast task result with a reasonable timeout
        boolean fastTaskTimedOut = false;
        try {
            fast.get(2, TimeUnit.SECONDS); // 2 seconds should be MORE than enough for instant task
        } catch (TimeoutException e) {
            fastTaskTimedOut = true;
        }

        System.out.println("Fast task timed out (starved): " + fastTaskTimedOut);
        assertThat(fastTaskTimedOut)
                .as("Fast task should be starved because slow tasks occupy all threads in the shared pool")
                .isTrue();
    }
}
