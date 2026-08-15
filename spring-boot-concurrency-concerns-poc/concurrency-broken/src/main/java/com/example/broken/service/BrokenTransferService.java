package com.example.broken.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Deliberately broken transfer service demonstrating deadlock.
 * 
 * The bug: locks are acquired in the order of the arguments (from → to).
 * When two threads do opposite transfers simultaneously:
 *   Thread 1: transfer(A, B) → locks A, then tries to lock B
 *   Thread 2: transfer(B, A) → locks B, then tries to lock A
 * 
 * This creates a circular wait → DEADLOCK.
 */
@Service
public class BrokenTransferService {

    private final Map<String, Long> balances = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void initCounter(String name, long value) {
        balances.put(name, value);
        locks.computeIfAbsent(name, k -> new ReentrantLock());
    }

    public long getBalance(String name) {
        return balances.getOrDefault(name, 0L);
    }

    /**
     * Transfer amount from one counter to another.
     * BUG: Always locks 'from' first, then 'to' — inconsistent ordering causes deadlock.
     */
    public void transfer(String from, String to, long amount) {
        ReentrantLock fromLock = locks.computeIfAbsent(from, k -> new ReentrantLock());
        ReentrantLock toLock = locks.computeIfAbsent(to, k -> new ReentrantLock());

        // BUG: Lock ordering follows argument order, not a global consistent order
        fromLock.lock();
        try {
            // Small delay to increase deadlock probability
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            toLock.lock();
            try {
                long fromBalance = balances.getOrDefault(from, 0L);
                long toBalance = balances.getOrDefault(to, 0L);
                balances.put(from, fromBalance - amount);
                balances.put(to, toBalance + amount);
            } finally {
                toLock.unlock();
            }
        } finally {
            fromLock.unlock();
        }
    }
}
