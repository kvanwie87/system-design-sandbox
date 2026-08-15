package com.example.customthreading.task;

import com.example.customthreading.annotation.MyScheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Demonstrates our custom @MyScheduled for periodic background tasks.
 *
 * How it works:
 * 1. MyScheduledBeanPostProcessor scans this bean during context initialization.
 * 2. It finds @MyScheduled on cleanup() and reads fixedRate=10000.
 * 3. It calls scheduledExecutor.scheduleAtFixedRate(...) to register periodic execution.
 * 4. Every 10 seconds, the ScheduledExecutorService invokes cleanup() via reflection.
 *
 * No proxy is created — unlike @MyAsync, scheduled tasks don't need interception because
 * the scheduler itself invokes the method, rather than application code calling it.
 */
@Component
public class ScheduledCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(ScheduledCleanupTask.class);

    @MyScheduled(fixedRate = 10000)
    public void cleanup() {
        log.info("[custom-threading] Cleanup executed at {}", LocalDateTime.now());
    }
}
