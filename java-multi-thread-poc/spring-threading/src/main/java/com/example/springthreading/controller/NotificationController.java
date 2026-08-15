package com.example.springthreading.controller;

import com.example.springthreading.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST endpoint for the fire-and-forget async use case.
 *
 * POST /notifications/send — accepts a JSON body with a "message" field,
 * delegates to the async service, and returns 202 Accepted immediately.
 * The actual notification processing happens on a background thread.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> send(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        // This call returns immediately — the proxy dispatches it to the thread pool
        notificationService.sendNotification(message);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("status", "accepted"));
    }
}
