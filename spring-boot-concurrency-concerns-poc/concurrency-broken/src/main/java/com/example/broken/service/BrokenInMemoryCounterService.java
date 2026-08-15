package com.example.broken.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Deliberately broken counter service demonstrating:
 * 1. Race condition: unsynchronized HashMap + plain long (read-modify-write without atomicity)
 * 2. Visibility issue: non-volatile flag that may never be seen by reader threads
 */
@Service
public class BrokenInMemoryCounterService {

    // BUG: HashMap is not thread-safe — concurrent puts can corrupt internal structure
    private final Map<String, Long> counters = new HashMap<>();

    // BUG: non-volatile flag — changes by one thread may never be visible to another
    private boolean shutdownRequested = false;

    public long increment(String name) {
        // BUG: read-modify-write is not atomic — classic race condition
        Long current = counters.getOrDefault(name, 0L);
        long newValue = current + 1;
        counters.put(name, newValue);
        return newValue;
    }

    public long decrement(String name) {
        Long current = counters.getOrDefault(name, 0L);
        long newValue = current - 1;
        counters.put(name, newValue);
        return newValue;
    }

    public long getValue(String name) {
        return counters.getOrDefault(name, 0L);
    }

    /**
     * Demonstrates visibility problem: writer sets flag but reader may never see it
     * because the field is not volatile.
     */
    public void requestShutdown() {
        shutdownRequested = true;
    }

    public boolean isShutdownRequested() {
        return shutdownRequested;
    }
}
