package com.example.customthreading.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom scheduled annotation that mirrors Spring's @Scheduled.
 * Methods annotated with @MyScheduled will be executed at a fixed rate
 * on a ScheduledExecutorService managed by MyScheduledBeanPostProcessor.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyScheduled {

    /**
     * Fixed rate in milliseconds between invocations.
     */
    long fixedRate();
}
