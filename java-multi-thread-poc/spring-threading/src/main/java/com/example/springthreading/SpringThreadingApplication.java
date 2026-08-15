package com.example.springthreading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the spring-threading module (port 8080).
 *
 * This module demonstrates Spring's built-in multithreading support:
 * - @EnableAsync activates proxy-based interception of @Async methods via AsyncAnnotationBeanPostProcessor.
 *   Under the hood, Spring wraps beans with @Async methods in a proxy that submits calls to a TaskExecutor.
 * - @EnableScheduling activates ScheduledAnnotationBeanPostProcessor, which scans for @Scheduled methods
 *   and registers them with a TaskScheduler for periodic execution.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SpringThreadingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringThreadingApplication.class, args);
    }
}
