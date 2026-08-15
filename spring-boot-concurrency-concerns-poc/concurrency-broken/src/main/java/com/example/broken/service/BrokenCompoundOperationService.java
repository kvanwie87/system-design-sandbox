package com.example.broken.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Deliberately broken service demonstrating non-atomic compound operations
 * on a ConcurrentHashMap.
 * 
 * BUG: Even though ConcurrentHashMap is thread-safe for individual operations,
 * COMPOUND operations (check + read + write as separate calls) are NOT atomic.
 * Between the get() and put(), another thread can modify the same entry.
 * 
 * Common misconception: "I'm using ConcurrentHashMap so it's thread-safe!"
 * Reality: Only individual method calls are atomic. Multi-step logic is still racy.
 */
@Service
public class BrokenCompoundOperationService {

    private final ConcurrentMap<String, Long> counters = new ConcurrentHashMap<>();

    public void initCounter(String name, long value) {
        counters.put(name, value);
    }

    /**
     * BUG: get() + arithmetic + put() is NOT atomic even on ConcurrentHashMap.
     * Two threads can both get() the same value, compute independently, then put() —
     * one update is silently lost.
     */
    public long incrementBy(String name, long delta) {
        Long current = counters.get(name);
        if (current == null) {
            current = 0L;
        }
        long newValue = current + delta;
        counters.put(name, newValue);
        return newValue;
    }

    /**
     * BUG: containsKey() + get() is NOT atomic.
     * Between the check and the get, another thread can remove the entry → NPE.
     */
    public long getValueOrThrow(String name) {
        if (counters.containsKey(name)) {
            // Another thread can remove the entry RIGHT HERE
            return counters.get(name); // NPE possible!
        }
        throw new IllegalArgumentException("Counter not found: " + name);
    }

    /**
     * BUG: get() + conditional put() is NOT atomic.
     * "Only decrement if value > 0" — two threads can both see value=1,
     * both decrement, result goes to -1.
     */
    public long decrementIfPositive(String name) {
        Long current = counters.get(name);
        if (current != null && current > 0) {
            long newValue = current - 1;
            counters.put(name, newValue);
            return newValue;
        }
        return current != null ? current : 0;
    }

    public Long getValue(String name) {
        return counters.getOrDefault(name, 0L);
    }

    public void remove(String name) {
        counters.remove(name);
    }
}
