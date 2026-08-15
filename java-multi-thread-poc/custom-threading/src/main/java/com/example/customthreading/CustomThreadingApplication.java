package com.example.customthreading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the custom-threading module (port 8081).
 *
 * This module provides the same REST API as spring-threading, but WITHOUT using
 * Spring's @EnableAsync or @EnableScheduling. Instead, it uses:
 * - @MyAsync + MyAsyncBeanPostProcessor → JDK dynamic proxy + custom ExecutorService
 * - @MyScheduled + MyScheduledBeanPostProcessor → ScheduledExecutorService
 *
 * No @EnableAsync/@EnableScheduling needed here — our BeanPostProcessors handle everything.
 */
@SpringBootApplication
public class CustomThreadingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomThreadingApplication.class, args);
    }
}
