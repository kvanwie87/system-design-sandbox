package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.SessionService;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<Void> putAttribute(@PathVariable String sessionId,
                                             @RequestParam String attributeKey,
                                             @RequestBody String attributeValue,
                                             @RequestParam(required = false) Long ttlSeconds) {
        sessionService.putAttribute(sessionId, attributeKey, attributeValue, ttlSeconds);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<Object, Object>> getAttributes(@PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.getAttributes(sessionId));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
        return ResponseEntity.ok().build();
    }
}
