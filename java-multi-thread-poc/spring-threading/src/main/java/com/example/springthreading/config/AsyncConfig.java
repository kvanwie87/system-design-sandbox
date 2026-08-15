package com.example.springthreading.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configures the thread pool that backs @Async method execution.
 *
 * When Spring's @EnableAsync is active, it looks for a TaskExecutor bean to use
 * as the default executor for @Async methods. If none is defined, Spring falls back
 * to a SimpleAsyncTaskExecutor (unbounded thread creation — not ideal for production).
 *
 * Here we provide a bounded ThreadPoolTaskExecutor:
 * - corePoolSize=4: four threads always alive in the pool
 * - maxPoolSize=4: won't grow beyond 4 (tasks queue instead)
 * - queueCapacity=100: up to 100 tasks can wait before rejection
 * - threadNamePrefix="async-": makes it easy to identify async threads in logs
 */
@Configuration
public class AsyncConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
