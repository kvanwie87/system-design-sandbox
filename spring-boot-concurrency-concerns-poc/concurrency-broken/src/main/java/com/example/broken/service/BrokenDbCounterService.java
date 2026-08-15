package com.example.broken.service;

import com.example.broken.entity.BrokenCounterEntity;
import com.example.broken.repository.BrokenCounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deliberately broken DB counter service demonstrating lost updates.
 * 
 * The read-modify-write pattern here is NOT atomic:
 * 1. Thread A reads counter value = 5
 * 2. Thread B reads counter value = 5
 * 3. Thread A writes value = 6
 * 4. Thread B writes value = 6 (Thread A's update is LOST)
 *
 * No optimistic locking (@Version) or pessimistic locking (SELECT FOR UPDATE) is used,
 * so concurrent transactions silently overwrite each other's changes.
 */
@Service
public class BrokenDbCounterService {

    private final BrokenCounterRepository counterRepository;

    public BrokenDbCounterService(BrokenCounterRepository counterRepository) {
        this.counterRepository = counterRepository;
    }

    @Transactional
    public BrokenCounterEntity increment(String name) {
        BrokenCounterEntity counter = counterRepository.findByName(name)
                .orElseGet(() -> counterRepository.saveAndFlush(new BrokenCounterEntity(name, 0L)));

        // BUG: Read-modify-write without any locking
        // Multiple threads can read the same value and overwrite each other
        counter.setValue(counter.getValue() + 1);
        return counterRepository.saveAndFlush(counter);
    }

    @Transactional
    public BrokenCounterEntity decrement(String name) {
        BrokenCounterEntity counter = counterRepository.findByName(name)
                .orElseGet(() -> counterRepository.saveAndFlush(new BrokenCounterEntity(name, 0L)));
        counter.setValue(counter.getValue() - 1);
        return counterRepository.saveAndFlush(counter);
    }

    @Transactional(readOnly = true)
    public BrokenCounterEntity getValue(String name) {
        return counterRepository.findByName(name)
                .orElse(new BrokenCounterEntity(name, 0L));
    }
}
