package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.PubSubService;

@RestController
@RequestMapping("/api/pubsub")
public class PubSubController {

    private final PubSubService pubSubService;

    public PubSubController(PubSubService pubSubService) {
        this.pubSubService = pubSubService;
    }

    @PostMapping("/{channel}")
    public ResponseEntity<Void> publish(@PathVariable String channel, @RequestBody String message) {
        pubSubService.publish(channel, message);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{channel}")
    public ResponseEntity<List<String>> getMessages(@PathVariable String channel) {
        return ResponseEntity.ok(pubSubService.getMessages(channel));
    }

    @DeleteMapping("/{channel}")
    public ResponseEntity<Void> clearMessages(@PathVariable String channel) {
        pubSubService.clearMessages(channel);
        return ResponseEntity.ok().build();
    }
}
