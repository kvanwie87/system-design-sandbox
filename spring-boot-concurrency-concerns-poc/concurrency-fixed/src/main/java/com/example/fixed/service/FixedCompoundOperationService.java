package com.example.fixed.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Fixed service demonstrating atomic compound operations on ConcurrentHashMap.
 * 
 * FIX: Use compute(), merge(), computeIfPresent() — these execute the entire
 * read-modify-write as a single atomic operation with respect to other threads
 * operating on the same key.
 */
@Service
public class FixedCompoundOperationService {

    private final ConcurrentMap<String, Long> counters = new ConcurrentHashMap<>();

    public void initCounter(String name, long value) {
        counters.put(name, value);
    }

    /**
     * FIX: merge() performs get + add + put atomically.
     * The remapping function runs under the key's lock segment.
     */
    public long incrementBy(String name, long delta) {
        return counters.merge(name, delta, Long::sum);
    }

    /**
     * FIX: Use getOrDefault or compute — no separate containsKey check.
     * If the entry might be removed concurrently, handle null from get() directly.
     */
    public long getValueOrThrow(String name) {
        Long value = counters.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Counter not found: " + name);
        }
        return value;
    }

    /**
     * FIX: computeIfPresent() performs the conditional update atomically.
     * The function only runs if the key is present, and the entire operation is atomic.
     */
    public long decrementIfPositive(String name) {
        Long result = counters.computeIfPresent(name, (k, current) -> {
            if (current > 0) {
                return current - 1;
            }
            return current; // Don't go below zero
        });
        return result != null ? result : 0;
    }

    public Long getValue(String name) {
        return counters.getOrDefault(name, 0L);
    }

    public void remove(String name) {
        counters.remove(name);
    }
}
