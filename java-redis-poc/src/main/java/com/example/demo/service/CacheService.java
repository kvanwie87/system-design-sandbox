package com.example.demo.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    @Cacheable(value = "demoCache", key = "#key")
    public String computeValue(String key) {
        // Simulate expensive computation
        return "computed-" + key + "-" + System.currentTimeMillis();
    }

    @CacheEvict(value = "demoCache", key = "#key")
    public void evict(String key) {
        // Eviction handled by annotation
    }
}
