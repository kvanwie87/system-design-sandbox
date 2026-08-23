package com.example.demo.dto;

public record RateLimitInfo(boolean allowed, int remaining, long retryAfterSeconds) {
}
