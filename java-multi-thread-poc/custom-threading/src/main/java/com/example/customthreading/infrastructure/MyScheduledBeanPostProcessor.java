package com.example.customthreading.infrastructure;

import com.example.customthreading.annotation.MyScheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Custom BeanPostProcessor that replicates Spring's ScheduledAnnotationBeanPostProcessor.
 *
 * How Spring does it (for comparison):
 * 1. ScheduledAnnotationBeanPostProcessor scans each bean for @Scheduled methods.
 * 2. It parses the annotation attributes (fixedRate, fixedDelay, cron, etc.).
 * 3. It registers each method as a ScheduledTask with the TaskScheduler.
 * 4. On context close, it cancels all scheduled tasks and shuts down the scheduler.
 *
 * Our simplified version:
 * 1. In postProcessAfterInitialization, scan the bean's declared methods for @MyScheduled.
 * 2. For each match, call scheduleAtFixedRate on our ScheduledExecutorService.
 * 3. On destroy(), shut down the executor gracefully.
 *
 * Key difference from @MyAsync: this BPP does NOT wrap the bean in a proxy.
 * It only registers the methods for periodic invocation — no interception needed
 * because scheduled methods are invoked by the scheduler, not by application code.
 */
@Component
public class MyScheduledBeanPostProcessor implements BeanPostProcessor, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(MyScheduledBeanPostProcessor.class);

    private final ScheduledExecutorService scheduledExecutor;

    public MyScheduledBeanPostProcessor(@Qualifier("myScheduledExecutor") ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();

        for (Method method : beanClass.getDeclaredMethods()) {
            MyScheduled annotation = method.getAnnotation(MyScheduled.class);
            if (annotation != null) {
                long fixedRate = annotation.fixedRate();
                log.info("Scheduling method '{}.{}' at fixedRate={}ms", beanName, method.getName(), fixedRate);

                // Register for periodic execution — first execution after fixedRate ms,
                // then every fixedRate ms thereafter
                scheduledExecutor.scheduleAtFixedRate(() -> {
                    try {
                        method.setAccessible(true);
                        method.invoke(bean);
                    } catch (Exception e) {
                        log.error("Scheduled execution failed for method '{}.{}'", beanName, method.getName(), e);
                    }
                }, fixedRate, fixedRate, TimeUnit.MILLISECONDS);
            }
        }

        // Return the bean unmodified — no proxy needed for scheduled tasks
        return bean;
    }

    @Override
    public void destroy() {
        log.info("Shutting down myScheduledExecutor");
        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
