package com.example.springthreading.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Demonstrates the "fire-and-forget" async pattern using Spring's @Async.
 *
 * How it works under the hood:
 * 1. Spring's AsyncAnnotationBeanPostProcessor detects @Async on sendNotification().
 * 2. It wraps this bean in a proxy (CGLIB since there's no interface).
 * 3. When the controller calls sendNotification(), the proxy intercepts the call,
 *    submits it as a Runnable to the configured TaskExecutor, and returns void immediately.
 * 4. The actual method body executes on a pool thread (named "async-N").
 *
 * Key constraint: @Async only works when called through the proxy — internal self-calls
 * (this.sendNotification()) bypass the proxy and execute synchronously.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Async
    public void sendNotification(String message) {
        log.info("Starting async notification on thread: {}", Thread.currentThread().getName());
        try {
            // Simulate slow work (e.g., sending an email or pushing to a queue)
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Notification sent: '{}' on thread: {}", message, Thread.currentThread().getName());
    }
}
