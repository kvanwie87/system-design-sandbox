package com.example.fixed.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe counter service demonstrating fixes for:
 * 1. Race condition: ConcurrentHashMap + AtomicLong (lock-free atomic increment)
 * 2. Visibility issue: volatile flag guarantees cross-thread visibility
 */
@Service
public class FixedInMemoryCounterService {

    // FIX: ConcurrentHashMap is thread-safe for concurrent access
    private final ConcurrentMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    // FIX: volatile ensures writes are immediately visible to all threads
    private volatile boolean shutdownRequested = false;

    public long increment(String name) {
        // FIX: computeIfAbsent is atomic, and AtomicLong.incrementAndGet is lock-free atomic
        return counters.computeIfAbsent(name, k -> new AtomicLong(0L)).incrementAndGet();
    }

    public long decrement(String name) {
        return counters.computeIfAbsent(name, k -> new AtomicLong(0L)).decrementAndGet();
    }

    public long getValue(String name) {
        AtomicLong counter = counters.get(name);
        return counter != null ? counter.get() : 0L;
    }

    /**
     * Demonstrates visibility fix: volatile guarantees that when one thread writes true,
     * every other thread will see that update on its next read.
     */
    public void requestShutdown() {
        shutdownRequested = true;
    }

    public boolean isShutdownRequested() {
        return shutdownRequested;
    }
}
