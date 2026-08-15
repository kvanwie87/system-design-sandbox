package com.example.fixed.service;

import com.example.common.entity.CounterEntity;
import com.example.common.repository.CounterRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixed DB counter service demonstrating two concurrency solutions:
 * 
 * 1. Optimistic locking (@Version): reads entity, modifies, saves — if another transaction
 *    modified the same row, @Version mismatch throws OptimisticLockException. We retry
 *    with a bounded retry loop, each attempt in a NEW transaction so we get fresh data.
 * 
 * 2. Pessimistic locking (SELECT FOR UPDATE): acquires a row-level lock on read,
 *    blocking other transactions until the lock is released. No retry needed.
 */
@Service
public class FixedDbCounterService {

    private static final int MAX_RETRIES = 100;

    private final CounterRepository counterRepository;

    public FixedDbCounterService(CounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    /**
     * Increment using optimistic locking with retry.
     * Each attempt runs in its own transaction (REQUIRES_NEW) to get fresh entity state.
     * If a concurrent transaction modified the row, @Version check fails and we retry.
     */
    public CounterEntity incrementOptimistic(String name) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return doIncrementOptimistic(name);
            } catch (ObjectOptimisticLockingFailureException e) {
                // Version mismatch — another transaction modified this row, retry
                if (attempt == MAX_RETRIES - 1) {
                    throw new RuntimeException("Optimistic lock retry exhausted after " + MAX_RETRIES + " attempts", e);
                }
            }
        }
        throw new RuntimeException("Should not reach here");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CounterEntity doIncrementOptimistic(String name) {
        CounterEntity counter = counterRepository.findByName(name)
                .orElseGet(() -> counterRepository.saveAndFlush(new CounterEntity(name, 0L)));
        counter.setValue(counter.getValue() + 1);
        return counterRepository.saveAndFlush(counter);
    }

    /**
     * Increment using pessimistic locking (SELECT FOR UPDATE).
     * The row-level lock blocks other transactions, guaranteeing serialized access.
     */
    @Transactional
    public CounterEntity incrementPessimistic(String name) {
        CounterEntity counter = counterRepository.findByNameForUpdate(name)
                .orElseGet(() -> counterRepository.saveAndFlush(new CounterEntity(name, 0L)));
        counter.setValue(counter.getValue() + 1);
        return counterRepository.saveAndFlush(counter);
    }

    @Transactional
    public CounterEntity decrementPessimistic(String name) {
        CounterEntity counter = counterRepository.findByNameForUpdate(name)
                .orElseGet(() -> counterRepository.saveAndFlush(new CounterEntity(name, 0L)));
        counter.setValue(counter.getValue() - 1);
        return counterRepository.saveAndFlush(counter);
    }

    @Transactional(readOnly = true)
    public CounterEntity getValue(String name) {
        return counterRepository.findByName(name)
                .orElse(new CounterEntity(name, 0L));
    }
}
