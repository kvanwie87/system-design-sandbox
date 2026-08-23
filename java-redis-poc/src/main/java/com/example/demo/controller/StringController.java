package com.example.demo.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.StringService;

/**
 * Demonstrates Redis String operations through a feature flags / runtime config use case.
 *
 * Redis Strings are the simplest data type but support powerful operations:
 * SET, GET, DEL, SETNX (set-if-not-exists), GETSET (atomic swap), and TTL.
 * Feature flags are a natural fit — boolean/string values that can be toggled
 * at runtime without redeployment, optionally with an expiration for time-limited features.
 */
@RestController
@RequestMapping("/api/flags")
public class StringController {

    private final StringService stringService;

    public StringController(StringService stringService) {
        this.stringService = stringService;
    }

    /**
     * Create or update a feature flag.
     * Example: PUT /api/flags/dark-mode?value=true
     * Example: PUT /api/flags/promo-banner?value=Summer+Sale&ttlSeconds=86400
     */
    @PutMapping("/{flagName}")
    public ResponseEntity<Void> setFlag(@PathVariable String flagName,
                                        @RequestParam String value,
                                        @RequestParam(required = false) Long ttlSeconds) {
        stringService.setFlag(flagName, value, ttlSeconds);
        return ResponseEntity.ok().build();
    }

    /**
     * Create a flag only if it doesn't already exist (SETNX).
     * Returns whether the flag was created or already existed.
     */
    @PostMapping("/{flagName}")
    public ResponseEntity<Map<String, Object>> createIfAbsent(@PathVariable String flagName,
                                                              @RequestParam String value,
                                                              @RequestParam(required = false) Long ttlSeconds) {
        boolean created = stringService.createIfAbsent(flagName, value, ttlSeconds);
        return ResponseEntity.ok(Map.of("flagName", flagName, "created", created));
    }

    /**
     * Get a feature flag value.
     */
    @GetMapping("/{flagName}")
    public ResponseEntity<Map<String, Object>> getFlag(@PathVariable String flagName) {
        return ResponseEntity.ok(stringService.getFlag(flagName));
    }

    /**
     * Toggle a boolean flag (true/false).
     */
    @PostMapping("/{flagName}/toggle")
    public ResponseEntity<Map<String, Object>> toggleFlag(@PathVariable String flagName) {
        return ResponseEntity.ok(stringService.toggleFlag(flagName));
    }

    /**
     * Delete a feature flag.
     */
    @DeleteMapping("/{flagName}")
    public ResponseEntity<Void> deleteFlag(@PathVariable String flagName) {
        stringService.deleteFlag(flagName);
        return ResponseEntity.ok().build();
    }

    /**
     * List all known feature flag names.
     */
    @GetMapping
    public ResponseEntity<Set<String>> listFlags() {
        return ResponseEntity.ok(stringService.listFlags());
    }
}
