package com.example.telemetry.ingest.dto;

/**
 * DTO for incoming sensor reading REST requests.
 * The batteryLevel field is optional (null for wired sensors) — demonstrates schema evolution.
 */
public record SensorReadingRequest(
        String sensorId,
        double temperature,
        double humidity,
        String location,
        Double batteryLevel
) {}
