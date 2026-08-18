package com.example.common.util;

import java.util.UUID;

/**
 * Utility for generating unique identifiers across services.
 * Uses UUID v4 with optional prefix for readability.
 */
public final class IdGenerator {

    private IdGenerator() {
        // utility class
    }

    /**
     * Generates a random UUID string.
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates an ID with a prefix for entity-type identification.
     * Example: "ord_550e8400-e29b-41d4-a716-446655440000"
     */
    public static String generate(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return generate();
        }
        return prefix + "_" + UUID.randomUUID().toString();
    }
}
