package com.example.springthreading.controller;

import com.example.springthreading.model.TaskStatus;
import com.example.springthreading.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for the async-with-polling use case.
 *
 * Flow:
 * 1. POST /reports/generate — creates a task ID, registers it as PENDING,
 *    kicks off async report generation, returns 202 with the task ID.
 * 2. GET /reports/{id} — client polls this until status is COMPLETE.
 *    Returns 404 if the task ID is unknown.
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
        // This returns a CompletableFuture, but we intentionally don't await it —
        // the result will be written to the task store when done
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
