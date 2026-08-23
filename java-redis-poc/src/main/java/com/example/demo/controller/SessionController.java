package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.SessionService;

/**
 * Demonstrates Redis-backed session management with:
 * - Auto-generated session IDs (UUID)
 * - Sliding TTL expiration (30 min, refreshed on access)
 * - Login/use/logout lifecycle
 *
 * Note: In production, use spring-session-data-redis which handles this
 * transparently via HttpSession integration.
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Create a new session (login). Returns the generated session token.
     * Example: POST /api/sessions with body {"username":"alice","role":"admin"}
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createSession(@RequestBody Map<String, String> attributes) {
        String sessionId = sessionService.createSession(attributes);
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }

    /**
     * Get session data. Refreshes the sliding TTL.
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<Object, Object>> getSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.getSession(sessionId));
    }

    /**
     * Update session attributes (add/modify fields). Refreshes TTL.
     */
    @PutMapping("/{sessionId}")
    public ResponseEntity<Void> updateSession(@PathVariable String sessionId,
                                              @RequestBody Map<String, String> attributes) {
        sessionService.updateSession(sessionId, attributes);
        return ResponseEntity.ok().build();
    }

    /**
     * Destroy a session (logout).
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> destroySession(@PathVariable String sessionId) {
        sessionService.destroySession(sessionId);
        return ResponseEntity.ok().build();
    }
}
