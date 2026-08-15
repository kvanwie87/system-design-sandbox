package com.example.springthreading.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Demonstrates Spring's @Scheduled for periodic background tasks.
 *
 * How it works under the hood:
 * 1. ScheduledAnnotationBeanPostProcessor scans this bean during context initialization.
 * 2. It finds the @Scheduled annotation on cleanup() and reads fixedRate=10000.
 * 3. It registers the method with a TaskScheduler (default: single-threaded ScheduledExecutorService).
 * 4. Every 10 seconds, the scheduler invokes cleanup() on its thread pool.
 *
 * fixedRate vs fixedDelay:
 * - fixedRate: invocations are spaced 10s apart regardless of execution duration.
 * - fixedDelay: next invocation starts 10s after the previous one finishes.
 */
@Component
public class ScheduledCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCleanupTask.class);

    @Scheduled(fixedRate = 10000)
    public void cleanup() {
        log.info("[spring-threading] Cleanup executed at {}", LocalDateTime.now());
    }
}
