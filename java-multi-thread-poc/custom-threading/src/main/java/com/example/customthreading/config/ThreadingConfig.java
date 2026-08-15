package com.example.customthreading.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configures the two executor pools used by the custom threading infrastructure.
 *
 * Mirrors how Spring internally separates async execution from scheduled execution:
 * - Spring uses a TaskExecutor for @Async (backed by ThreadPoolTaskExecutor)
 * - Spring uses a TaskScheduler for @Scheduled (backed by ScheduledThreadPoolExecutor)
 *
 * We do the same with raw JDK executors:
 * - myAsyncExecutor: 4-thread fixed pool for @MyAsync fire-and-forget / future work
 * - myScheduledExecutor: 2-thread scheduled pool for @MyScheduled periodic tasks
 *
 * Both use daemon threads so they won't prevent JVM shutdown.
 */
@Configuration
public class ThreadingConfig {

    /**
     * Thread pool for @MyAsync methods.
     * Same role as Spring's TaskExecutor — receives submitted Runnables from the async proxy.
     */
    @Bean(name = "myAsyncExecutor")
    public ExecutorService myAsyncExecutor() {
        AtomicInteger counter = new AtomicInteger(0);
        ThreadFactory factory = r -> {
            Thread t = new Thread(r);
            t.setName("my-async-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        return new ThreadPoolExecutor(
                4, 4, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                factory
        );
    }

    /**
     * Scheduled thread pool for @MyScheduled methods.
     * Same role as Spring's TaskScheduler — executes methods at fixed rates.
     */
    @Bean(name = "myScheduledExecutor")
    public ScheduledExecutorService myScheduledExecutor() {
        AtomicInteger counter = new AtomicInteger(0);
        ThreadFactory factory = r -> {
            Thread t = new Thread(r);
            t.setName("my-scheduled-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        return Executors.newScheduledThreadPool(2, factory);
    }
}
