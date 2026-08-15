package com.example.common.controller;

import com.example.common.dto.CounterResponse;
import com.example.common.dto.TransferRequest;
import org.springframework.web.bind.annotation.*;

public interface DbCounterController {

    @PostMapping("/db/counters/{name}/increment")
    CounterResponse increment(@PathVariable String name);

    @PostMapping("/db/counters/{name}/decrement")
    CounterResponse decrement(@PathVariable String name);

    @GetMapping("/db/counters/{name}")
    CounterResponse getValue(@PathVariable String name);

    @PostMapping("/db/counters/transfer")
    CounterResponse transfer(@RequestBody TransferRequest request);
}
