package com.example.fixed.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed service demonstrating the correct way to handle Check-Then-Act.
 * 
 * FIX: Use ConcurrentHashMap.computeIfAbsent() which performs the check-and-create
 * as a single atomic operation. The mapping function is called at most once per key,
 * even under concurrent access.
 */
@Service
public class FixedCheckThenActService {

    private final ConcurrentMap<String, Long> counters = new ConcurrentHashMap<>();

    // Tracks how many times initialization was performed (should be exactly 1 per counter)
    private final AtomicInteger initializationCount = new AtomicInteger(0);

    /**
     * FIX: computeIfAbsent is atomic — the mapping function executes at most once per key.
     * No window of vulnerability between check and create.
     */
    public long getOrCreate(String name, long initialValue) {
        counters.computeIfAbsent(name, k -> {
            initializationCount.incrementAndGet();
            return initialValue;
        });
        return counters.get(name);
    }

    public int getInitializationCount() {
        return initializationCount.get();
    }

    public void reset() {
        counters.clear();
        initializationCount.set(0);
    }
}
