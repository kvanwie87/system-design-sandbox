package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.PubSubService;

/**
 * Demonstrates Redis Pub/Sub through a domain event notification system.
 *
 * Channels represent event types (e.g., "order.created", "user.registered").
 * Publishers emit events, and the in-app listener stores received events
 * for inspection — simulating how microservices use Redis Pub/Sub for
 * lightweight, decoupled inter-service communication.
 *
 * Note: Redis Pub/Sub is fire-and-forget. If no subscriber is listening
 * when a message is published, it's lost. For durable messaging, use
 * Redis Streams or a dedicated message broker.
 */
@RestController
@RequestMapping("/api/events")
public class PubSubController {

    private final PubSubService pubSubService;

    public PubSubController(PubSubService pubSubService) {
        this.pubSubService = pubSubService;
    }

    /**
     * Publish a domain event.
     * Example: POST /api/events/order.created {"orderId":"123","total":59.99,"customer":"alice"}
     */
    @PostMapping("/{eventType}")
    public ResponseEntity<Void> publish(@PathVariable String eventType,
                                        @RequestBody Map<String, Object> payload) {
        pubSubService.publish(eventType, payload);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all received events for an event type (from the in-memory listener log).
     */
    @GetMapping("/{eventType}")
    public ResponseEntity<List<String>> getEvents(@PathVariable String eventType) {
        return ResponseEntity.ok(pubSubService.getMessages(eventType));
    }

    /**
     * Get all event types that have received messages.
     */
    @GetMapping
    public ResponseEntity<List<String>> getEventTypes() {
        return ResponseEntity.ok(pubSubService.getChannels());
    }

    /**
     * Clear the event log for an event type.
     */
    @DeleteMapping("/{eventType}")
    public ResponseEntity<Void> clearEvents(@PathVariable String eventType) {
        pubSubService.clearMessages(eventType);
        return ResponseEntity.ok().build();
    }
}
