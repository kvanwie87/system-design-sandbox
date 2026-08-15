package com.example.telemetry.ingest.controller;

import com.example.telemetry.ingest.dto.SensorReadingRequest;
import com.example.telemetry.ingest.service.SensorProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sensors")
public class SensorIngestController {

    private static final Logger log = LoggerFactory.getLogger(SensorIngestController.class);

    private final SensorProducerService producerService;

    public SensorIngestController(SensorProducerService producerService) {
        this.producerService = producerService;
    }

    /**
     * POST /api/sensors/readings
     * Accepts a JSON sensor reading and publishes it to Kafka transactionally.
     */
    @PostMapping("/readings")
    public ResponseEntity<Map<String, String>> publishReading(@RequestBody SensorReadingRequest request) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        try {
            log.info("[INGEST] traceId={} | Received reading: sensor={} temp={}C humidity={}% location={}",
                    traceId, request.sensorId(), request.temperature(), request.humidity(), request.location());

            producerService.sendReading(request, traceId);

            log.info("[INGEST] traceId={} | Accepted and queued for sensor.data topic", traceId);

            return ResponseEntity.accepted().body(Map.of(
                    "status", "accepted",
                    "traceId", traceId,
                    "sensorId", request.sensorId(),
                    "message", "Reading queued for publishing to sensor.data topic"
            ));
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * POST /api/sensors/simulate?sensorId=sensor-01&location=warehouse-A&count=100&intervalMs=500
     * Simulates bulk sensor data generation.
     */
    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulateReadings(
            @RequestParam(defaultValue = "sensor-001") String sensorId,
            @RequestParam(defaultValue = "warehouse-A") String location,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "100") long intervalMs) throws InterruptedException {

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        try {
            log.info("[INGEST] traceId={} | Simulate request: sensor={} count={} intervalMs={}",
                    traceId, sensorId, count, intervalMs);

            producerService.sendBatch(sensorId, location, count, intervalMs, traceId);

            log.info("[INGEST] traceId={} | Simulation complete: {} readings sent", traceId, count);

            return ResponseEntity.ok(Map.of(
                    "status", "completed",
                    "traceId", traceId,
                    "sensorId", sensorId,
                    "count", count,
                    "message", String.format("Sent %d simulated readings for sensor %s", count, sensorId)
            ));
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * POST /api/sensors/readings/v2
     * Demonstrates schema evolution: sends a reading with the new optional batteryLevel field.
     */
    @PostMapping("/readings/v2")
    public ResponseEntity<Map<String, String>> publishReadingV2(@RequestBody SensorReadingRequest request) {
        if (request.batteryLevel() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "batteryLevel is required for v2 endpoint (use /readings for wired sensors)"
            ));
        }
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        try {
            log.info("[INGEST] traceId={} | Received v2 reading: sensor={} temp={}C battery={}%",
                    traceId, request.sensorId(), request.temperature(), request.batteryLevel());

            producerService.sendReading(request, traceId);

            log.info("[INGEST] traceId={} | v2 reading accepted and queued", traceId);

            return ResponseEntity.accepted().body(Map.of(
                    "status", "accepted",
                    "traceId", traceId,
                    "sensorId", request.sensorId(),
                    "schemaVersion", "v2",
                    "message", "Reading with batteryLevel queued for sensor.data topic"
            ));
        } finally {
            MDC.remove("traceId");
        }
    }
}
