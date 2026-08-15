package com.example.fixed.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Fix #1: Stateless singleton — no instance fields for per-request data.
 * 
 * All request state lives in local variables or is passed through method parameters.
 * The singleton has no mutable state, so it's inherently thread-safe.
 * 
 * This is the simplest and most performant fix. Prefer this approach when possible.
 */
@Service
public class StatelessRequestService {

    /**
     * Processes an entire request using only local state.
     * No instance fields are touched — safe for any number of concurrent calls.
     */
    public RequestResult processRequest(String user, List<Long> amounts) {
        // All state is local to this method invocation (on the thread's stack)
        long runningTotal = 0;
        List<String> operationLog = new ArrayList<>();

        for (long amount : amounts) {
            runningTotal += amount;
            operationLog.add(user + " added " + amount);
        }

        return new RequestResult(user, runningTotal, operationLog);
    }

    public record RequestResult(String user, long total, List<String> log) {
    }
}
