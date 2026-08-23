package com.example.demo.controller;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.SortedSetService;

@RestController
@RequestMapping("/api/sorted-sets")
public class SortedSetController {

    private final SortedSetService sortedSetService;

    public SortedSetController(SortedSetService sortedSetService) {
        this.sortedSetService = sortedSetService;
    }

    @PostMapping("/{key}")
    public ResponseEntity<Void> add(@PathVariable String key, @RequestParam String member,
                                    @RequestParam double score) {
        sortedSetService.add(key, member, score);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{key}")
    public ResponseEntity<Set<Object>> getAll(@PathVariable String key) {
        return ResponseEntity.ok(sortedSetService.getAll(key));
    }

    @GetMapping("/{key}/range")
    public ResponseEntity<Set<Object>> getRange(@PathVariable String key,
                                                @RequestParam long start, @RequestParam long end) {
        return ResponseEntity.ok(sortedSetService.getRange(key, start, end));
    }

    @GetMapping("/{key}/score/{member}")
    public ResponseEntity<Double> getScore(@PathVariable String key, @PathVariable String member) {
        return ResponseEntity.ok(sortedSetService.getScore(key, member));
    }

    @DeleteMapping("/{key}/{member}")
    public ResponseEntity<Void> remove(@PathVariable String key, @PathVariable String member) {
        sortedSetService.remove(key, member);
        return ResponseEntity.ok().build();
    }
}
