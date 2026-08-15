package com.example.telemetry.ingest.service;

import com.example.telemetry.avro.SensorReading;
import com.example.telemetry.ingest.dto.SensorReadingRequest;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class SensorProducerService {

    private static final Logger log = LoggerFactory.getLogger(SensorProducerService.class);
    private static final String TOPIC = "sensor.data";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final KafkaTemplate<String, SensorReading> kafkaTemplate;

    public SensorProducerService(KafkaTemplate<String, SensorReading> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a single sensor reading transactionally with trace ID propagated via Kafka headers.
     */
    public CompletableFuture<SendResult<String, SensorReading>> sendReading(SensorReadingRequest request, String traceId) {
        SensorReading reading = buildAvroReading(request);

        return kafkaTemplate.executeInTransaction(template -> {
            ProducerRecord<String, SensorReading> record =
                    new ProducerRecord<>(TOPIC, null, null, request.sensorId(), reading);
            record.headers().add(TRACE_ID_HEADER, traceId.getBytes(StandardCharsets.UTF_8));

            CompletableFuture<SendResult<String, SensorReading>> future = template.send(record);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[PRODUCE] traceId={} | FAILED sensor={} error={}",
                            traceId, request.sensorId(), ex.getMessage());
                } else {
                    log.info("[PRODUCE] traceId={} | SENT sensor={} topic={} partition={} offset={}",
                            traceId, request.sensorId(), TOPIC,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
            return future;
        });
    }

    /**
     * Sends a batch of simulated sensor readings transactionally.
     */
    public void sendBatch(String sensorId, String location, int count, long intervalMs, String batchTraceId)
            throws InterruptedException {
        kafkaTemplate.executeInTransaction(template -> {
            for (int i = 0; i < count; i++) {
                String traceId = batchTraceId + "-" + (i + 1);
                SensorReading reading = SensorReading.newBuilder()
                        .setSensorId(sensorId)
                        .setTemperature(20.0 + Math.random() * 15.0)
                        .setHumidity(40.0 + Math.random() * 40.0)
                        .setTimestamp(Instant.now())
                        .setLocation(location)
                        .build();

                ProducerRecord<String, SensorReading> record =
                        new ProducerRecord<>(TOPIC, null, null, sensorId, reading);
                record.headers().add(TRACE_ID_HEADER, traceId.getBytes(StandardCharsets.UTF_8));

                template.send(record);
                log.debug("[PRODUCE] traceId={} | SENT batch {}/{} sensor={}",
                        traceId, i + 1, count, sensorId);

                if (intervalMs > 0 && i < count - 1) {
                    try {
                        Thread.sleep(intervalMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Simulation interrupted", e);
                    }
                }
            }
            log.info("[PRODUCE] traceId={} | Batch complete: {} readings for sensor {}",
                    batchTraceId, count, sensorId);
            return null;
        });
    }

    private SensorReading buildAvroReading(SensorReadingRequest request) {
        SensorReading.Builder builder = SensorReading.newBuilder()
                .setSensorId(request.sensorId())
                .setTemperature(request.temperature())
                .setHumidity(request.humidity())
                .setTimestamp(Instant.now())
                .setLocation(request.location());

        if (request.batteryLevel() != null) {
            builder.setBatteryLevel(request.batteryLevel());
        }

        return builder.build();
    }
}
