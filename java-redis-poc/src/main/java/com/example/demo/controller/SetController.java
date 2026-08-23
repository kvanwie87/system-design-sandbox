package com.example.demo.controller;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.SetService;

@RestController
@RequestMapping("/api/sets")
public class SetController {

    private final SetService setService;

    public SetController(SetService setService) {
        this.setService = setService;
    }

    @PostMapping("/{key}")
    public ResponseEntity<Void> add(@PathVariable String key, @RequestBody String member) {
        setService.add(key, member);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{key}")
    public ResponseEntity<Set<Object>> getAll(@PathVariable String key) {
        return ResponseEntity.ok(setService.getAll(key));
    }

    @GetMapping("/{key}/member/{member}")
    public ResponseEntity<Boolean> isMember(@PathVariable String key, @PathVariable String member) {
        return ResponseEntity.ok(setService.isMember(key, member));
    }

    @DeleteMapping("/{key}/{member}")
    public ResponseEntity<Void> remove(@PathVariable String key, @PathVariable String member) {
        setService.remove(key, member);
        return ResponseEntity.ok().build();
    }
}
