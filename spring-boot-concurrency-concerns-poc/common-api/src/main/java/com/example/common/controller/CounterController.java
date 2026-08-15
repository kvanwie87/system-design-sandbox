package com.example.common.controller;

import com.example.common.dto.CounterResponse;
import com.example.common.dto.TransferRequest;
import org.springframework.web.bind.annotation.*;

public interface CounterController {

    @PostMapping("/counters/{name}/increment")
    CounterResponse increment(@PathVariable String name);

    @PostMapping("/counters/{name}/decrement")
    CounterResponse decrement(@PathVariable String name);

    @GetMapping("/counters/{name}")
    CounterResponse getValue(@PathVariable String name);

    @PostMapping("/counters/transfer")
    CounterResponse transfer(@RequestBody TransferRequest request);
}
