package com.example.customthreading.infrastructure;

import com.example.customthreading.annotation.MyAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Custom BeanPostProcessor that replicates Spring's AsyncAnnotationBeanPostProcessor.
 *
 * How Spring does it (for comparison):
 * 1. AsyncAnnotationBeanPostProcessor extends AbstractAdvisingBeanPostProcessor
 * 2. It creates an AsyncAnnotationAdvisor with a PointcutAdvisor matching @Async methods
 * 3. It wraps the bean in a Spring AOP proxy (CGLIB or JDK dynamic proxy)
 * 4. The advice intercepts calls and submits them to a TaskExecutor
 *
 * Our simplified version:
 * 1. In postProcessAfterInitialization, scan the bean class for methods with @MyAsync
 * 2. If found, create a JDK dynamic proxy implementing the bean's interfaces
 * 3. The InvocationHandler intercepts @MyAsync methods and dispatches them to our ExecutorService
 * 4. For void methods: fire-and-forget (submit and return null)
 * 5. For CompletableFuture methods: submit and return a future that completes when work finishes
 *
 * Limitation: JDK dynamic proxies require the bean to implement at least one interface.
 * Spring's version can use CGLIB to proxy concrete classes — we skip that for simplicity.
 */
@Component
public class MyAsyncBeanPostProcessor implements BeanPostProcessor, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(MyAsyncBeanPostProcessor.class);

    private final ExecutorService asyncExecutor;

    public MyAsyncBeanPostProcessor(@Qualifier("myAsyncExecutor") ExecutorService asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();

        // Step 1: Scan for @MyAsync methods on the concrete class
        Set<String> asyncMethodNames = Arrays.stream(beanClass.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(MyAsync.class))
                .map(Method::getName)
                .collect(Collectors.toSet());

        if (asyncMethodNames.isEmpty()) {
            return bean; // No async methods — return the bean unmodified
        }

        log.info("Creating async proxy for bean '{}' — async methods: {}", beanName, asyncMethodNames);

        // Step 2: Resolve interfaces for JDK proxy
        Class<?>[] interfaces = beanClass.getInterfaces();
        if (interfaces.length == 0) {
            interfaces = getAllInterfaces(beanClass);
            if (interfaces.length == 0) {
                log.warn("Bean '{}' has @MyAsync methods but implements no interfaces — skipping proxy", beanName);
                return bean;
            }
        }

        // Step 3: Create JDK dynamic proxy with our custom InvocationHandler
        InvocationHandler handler = (proxy, method, args) -> {
            if (asyncMethodNames.contains(method.getName()) && isAsyncAnnotatedOnTarget(beanClass, method)) {
                return dispatchAsync(bean, method, args);
            }
            // Non-async methods pass through directly to the real bean
            return method.invoke(bean, args);
        };

        return Proxy.newProxyInstance(beanClass.getClassLoader(), interfaces, handler);
    }

    /**
     * Verify that the method on the actual bean class (not just matching by name)
     * has the @MyAsync annotation. Guards against name collisions across interfaces.
     */
    private boolean isAsyncAnnotatedOnTarget(Class<?> beanClass, Method proxyMethod) {
        try {
            Method targetMethod = beanClass.getMethod(proxyMethod.getName(), proxyMethod.getParameterTypes());
            return targetMethod.isAnnotationPresent(MyAsync.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Routes the method invocation to the async executor.
     * - CompletableFuture return type: wrap in supplyAsync so the caller gets a future
     * - void return type: submit as fire-and-forget (caller gets null immediately)
     */
    private Object dispatchAsync(Object bean, Method method, Object[] args) {
        if (CompletableFuture.class.isAssignableFrom(method.getReturnType())) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    @SuppressWarnings("unchecked")
                    CompletableFuture<Object> result = (CompletableFuture<Object>) method.invoke(bean, args);
                    return result.join(); // Block on the pool thread until the actual work completes
                } catch (Exception e) {
                    throw new RuntimeException("Async execution failed", e);
                }
            }, asyncExecutor);
        } else {
            // Fire-and-forget: submit to executor, return null to caller
            asyncExecutor.submit(() -> {
                try {
                    method.invoke(bean, args);
                } catch (Exception e) {
                    log.error("Async fire-and-forget execution failed for method '{}'", method.getName(), e);
                }
            });
            return null;
        }
    }

    /** Walk the class hierarchy to find all implemented interfaces. */
    private Class<?>[] getAllInterfaces(Class<?> clazz) {
        Set<Class<?>> interfaces = new java.util.LinkedHashSet<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            interfaces.addAll(Arrays.asList(current.getInterfaces()));
            current = current.getSuperclass();
        }
        return interfaces.toArray(new Class<?>[0]);
    }

    @Override
    public void destroy() {
        log.info("Shutting down myAsyncExecutor");
        asyncExecutor.shutdown();
    }
}
