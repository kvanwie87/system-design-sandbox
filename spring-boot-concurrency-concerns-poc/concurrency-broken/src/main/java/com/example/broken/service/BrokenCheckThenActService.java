package com.example.broken.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deliberately broken service demonstrating Check-Then-Act (TOCTOU) race condition.
 * 
 * BUG: The "check if exists, then create" pattern is NOT atomic.
 * Between the containsKey() check and the put(), another thread can slip in
 * and create the same entry — resulting in duplicate initialization, lost data,
 * or inconsistent state.
 * 
 * In a DB context this manifests as duplicate rows or constraint violations.
 */
@Service
public class BrokenCheckThenActService {

    private final Map<String, Long> counters = new HashMap<>();

    // Tracks how many times initialization was performed (should be 1 per counter)
    private final AtomicInteger initializationCount = new AtomicInteger(0);

    /**
     * BUG: check-then-act — two threads can both see the counter as absent
     * and both perform initialization.
     */
    public long getOrCreate(String name, long initialValue) {
        if (!counters.containsKey(name)) {
            // Window of vulnerability: another thread can also pass the check above
            try {
                Thread.sleep(0, 1); // Tiny delay to widen the race window
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            counters.put(name, initialValue);
            initializationCount.incrementAndGet();
        }
        return counters.getOrDefault(name, 0L);
    }

    public int getInitializationCount() {
        return initializationCount.get();
    }

    public void reset() {
        counters.clear();
        initializationCount.set(0);
    }
}
