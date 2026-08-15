package com.example.fixed.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fixed transfer service demonstrating deadlock prevention via ordered locking.
 * 
 * The fix: always acquire locks in a consistent global order (lexicographic by name).
 * Regardless of which direction the transfer goes, both threads will attempt to lock
 * the "smaller" name first, eliminating circular wait.
 * 
 * This breaks one of the four Coffman conditions for deadlock (circular wait),
 * making deadlock impossible.
 */
@Service
public class FixedTransferService {

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
     * FIX: Always lock in lexicographic order to prevent circular wait.
     */
    public void transfer(String from, String to, long amount) {
        ReentrantLock fromLock = locks.computeIfAbsent(from, k -> new ReentrantLock());
        ReentrantLock toLock = locks.computeIfAbsent(to, k -> new ReentrantLock());

        // FIX: Determine lock order by comparing counter names lexicographically
        ReentrantLock firstLock;
        ReentrantLock secondLock;
        if (from.compareTo(to) < 0) {
            firstLock = fromLock;
            secondLock = toLock;
        } else {
            firstLock = toLock;
            secondLock = fromLock;
        }

        firstLock.lock();
        try {
            // Same delay as broken version — but no deadlock because order is consistent
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            secondLock.lock();
            try {
                long fromBalance = balances.getOrDefault(from, 0L);
                long toBalance = balances.getOrDefault(to, 0L);
                balances.put(from, fromBalance - amount);
                balances.put(to, toBalance + amount);
            } finally {
                secondLock.unlock();
            }
        } finally {
            firstLock.unlock();
        }
    }
}
