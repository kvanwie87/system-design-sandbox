package com.example.customthreading.controller;

import com.example.customthreading.model.TaskStatus;
import com.example.customthreading.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for the async-with-polling use case (custom-threading module).
 *
 * Identical API to spring-threading's ReportController.
 * The injected ReportService is a JDK dynamic proxy — generateReport() executes
 * asynchronously on myAsyncExecutor, while registerTask()/getTaskStatus() pass through.
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generate() {
        String taskId = UUID.randomUUID().toString();
        // Register PENDING state before dispatching async work
        reportService.registerTask(taskId);
        // generateReport() is intercepted by the async proxy — returns immediately
        reportService.generateReport(taskId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("taskId", taskId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable String id) {
        TaskStatus status = reportService.getTaskStatus(id);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, String> response = Map.of(
                "status", status.getStatus().name(),
                "result", status.getResult() != null ? status.getResult() : ""
        );
        return ResponseEntity.ok(response);
    }
}
