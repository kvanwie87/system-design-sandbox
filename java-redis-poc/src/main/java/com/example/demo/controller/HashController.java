package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.HashService;

/**
 * Demonstrates Redis Hash operations through a user profile use case.
 * Each user profile is stored as a Redis Hash where:
 * - Key: user:{userId}
 * - Fields: name, email, age, etc.
 */
@RestController
@RequestMapping("/api/hashes")
public class HashController {

    private final HashService hashService;

    public HashController(HashService hashService) {
        this.hashService = hashService;
    }

    /**
     * Create or replace a user profile with all fields at once.
     * Example: PUT /api/hashes/users/1001 {"name":"Alice","email":"alice@example.com","age":"30"}
     */
    @PutMapping("/users/{userId}")
    public ResponseEntity<Void> saveProfile(@PathVariable String userId,
                                            @RequestBody Map<String, String> fields) {
        hashService.saveProfile(userId, fields);
        return ResponseEntity.ok().build();
    }

    /**
     * Update a single field of a user profile.
     * Example: PUT /api/hashes/users/1001/email with body "newemail@example.com"
     */
    @PutMapping("/users/{userId}/{field}")
    public ResponseEntity<Void> updateField(@PathVariable String userId,
                                            @PathVariable String field,
                                            @RequestBody String value) {
        hashService.updateField(userId, field, value);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all fields of a user profile (HGETALL).
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<Object, Object>> getProfile(@PathVariable String userId) {
        return ResponseEntity.ok(hashService.getProfile(userId));
    }

    /**
     * Get a single field of a user profile (HGET).
     */
    @GetMapping("/users/{userId}/{field}")
    public ResponseEntity<Object> getField(@PathVariable String userId, @PathVariable String field) {
        return ResponseEntity.ok(hashService.getField(userId, field));
    }

    /**
     * Delete a single field from a user profile (HDEL).
     */
    @DeleteMapping("/users/{userId}/{field}")
    public ResponseEntity<Void> deleteField(@PathVariable String userId, @PathVariable String field) {
        hashService.deleteField(userId, field);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete an entire user profile (DEL).
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String userId) {
        hashService.deleteProfile(userId);
        return ResponseEntity.ok().build();
    }
}
