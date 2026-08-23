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

@RestController
@RequestMapping("/api/hashes")
public class HashController {

    private final HashService hashService;

    public HashController(HashService hashService) {
        this.hashService = hashService;
    }

    @PutMapping("/{key}/{field}")
    public ResponseEntity<Void> putField(@PathVariable String key, @PathVariable String field,
                                         @RequestBody String value) {
        hashService.putField(key, field, value);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{key}")
    public ResponseEntity<Map<Object, Object>> getAll(@PathVariable String key) {
        return ResponseEntity.ok(hashService.getAll(key));
    }

    @GetMapping("/{key}/{field}")
    public ResponseEntity<Object> getField(@PathVariable String key, @PathVariable String field) {
        return ResponseEntity.ok(hashService.getField(key, field));
    }

    @DeleteMapping("/{key}/{field}")
    public ResponseEntity<Void> deleteField(@PathVariable String key, @PathVariable String field) {
        hashService.deleteField(key, field);
        return ResponseEntity.ok().build();
    }
}
