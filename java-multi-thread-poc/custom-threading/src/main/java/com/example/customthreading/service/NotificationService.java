package com.example.customthreading.service;

/**
 * Interface for the notification service.
 *
 * An interface is required because MyAsyncBeanPostProcessor uses JDK dynamic proxies,
 * which can only proxy interfaces (not concrete classes). This is a key difference from
 * Spring's @Async, which uses CGLIB to proxy concrete classes when no interface exists.
 */
public interface NotificationService {

    void sendNotification(String message);
}
