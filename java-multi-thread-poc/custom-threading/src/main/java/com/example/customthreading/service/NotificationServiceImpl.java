package com.example.customthreading.service;

import com.example.customthreading.annotation.MyAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Fire-and-forget notification service using our custom @MyAsync annotation.
 *
 * Behavior is identical to spring-threading's NotificationService:
 * - sendNotification() is intercepted by MyAsyncBeanPostProcessor's proxy
 * - The proxy submits the method to myAsyncExecutor (4-thread pool)
 * - The caller gets null back immediately (fire-and-forget)
 * - 2 seconds later, the notification completes on a "my-async-N" thread
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Override
    @MyAsync
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
