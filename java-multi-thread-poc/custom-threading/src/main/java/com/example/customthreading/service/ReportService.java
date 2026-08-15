package com.example.customthreading.service;

import com.example.customthreading.model.TaskStatus;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for the report generation service.
 *
 * Required for JDK dynamic proxy support in MyAsyncBeanPostProcessor.
 * The generateReport method is annotated with @MyAsync on the implementation class,
 * and the proxy intercepts it to execute asynchronously.
 */
public interface ReportService {

    CompletableFuture<String> generateReport(String taskId);

    void registerTask(String taskId);

    TaskStatus getTaskStatus(String taskId);
}
