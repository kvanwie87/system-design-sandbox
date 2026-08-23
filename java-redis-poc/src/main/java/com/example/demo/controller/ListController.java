package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.ListService;

@RestController
@RequestMapping("/api/lists")
public class ListController {

    private final ListService listService;

    public ListController(ListService listService) {
        this.listService = listService;
    }

    @PostMapping("/{key}")
    public ResponseEntity<Void> push(@PathVariable String key, @RequestBody String value,
                                     @RequestParam(defaultValue = "left") String direction) {
        listService.push(key, value, direction);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{key}")
    public ResponseEntity<List<Object>> getAll(@PathVariable String key) {
        return ResponseEntity.ok(listService.getAll(key));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Object> pop(@PathVariable String key,
                                      @RequestParam(defaultValue = "left") String direction) {
        return ResponseEntity.ok(listService.pop(key, direction));
    }
}
