package com.example.customthreading.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom async annotation that mirrors Spring's @Async.
 * Methods annotated with @MyAsync will be executed asynchronously
 * on a separate thread pool via a BeanPostProcessor proxy.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyAsync {
}
