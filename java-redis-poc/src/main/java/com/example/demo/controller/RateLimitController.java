package com.example.demo.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.RateLimitInfo;
import com.example.demo.service.RateLimitService;

@RestController
@RequestMapping("/api/rate-limit")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/check/{clientId}")
    public ResponseEntity<String> checkRateLimit(@PathVariable String clientId) {
        RateLimitInfo info = rateLimitService.checkRateLimit(clientId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Remaining", String.valueOf(info.remaining()));

        if (!info.allowed()) {
            headers.add("Retry-After", String.valueOf(info.retryAfterSeconds()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body("Rate limit exceeded");
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body("Request allowed");
    }
}
