package com.example.broken.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Deliberately broken singleton service that stores per-request state in instance fields.
 * 
 * BUG: Spring beans are singletons by default. Every concurrent request shares the same
 * instance, so fields like `currentUser` and `operationLog` are trampled by other threads.
 * 
 * Symptoms:
 * - User A sees User B's data
 * - Operation logs bleed across requests
 * - Intermittent "wrong user" errors that only appear under load
 */
@Service
public class BrokenSharedFieldService {

    // BUG: Mutable instance fields in a singleton — shared across ALL requests
    private String currentUser;
    private long runningTotal;
    private final List<String> operationLog = new ArrayList<>();

    /**
     * Simulates processing a request for a specific user.
     * Each "step" represents work done across the lifetime of a request.
     */
    public void beginRequest(String user) {
        this.currentUser = user;
        this.runningTotal = 0;
        this.operationLog.clear();
    }

    public void addAmount(long amount) {
        this.runningTotal += amount;
        this.operationLog.add(currentUser + " added " + amount);
    }

    public RequestResult finishRequest() {
        return new RequestResult(currentUser, runningTotal, List.copyOf(operationLog));
    }

    public record RequestResult(String user, long total, List<String> log) {
    }
}
