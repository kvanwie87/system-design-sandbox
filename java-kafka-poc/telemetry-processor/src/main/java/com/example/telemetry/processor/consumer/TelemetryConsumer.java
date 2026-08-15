package com.example.telemetry.processor.consumer;

import com.example.telemetry.avro.SensorReading;
import com.example.telemetry.processor.entity.SensorReadingEntity;
import com.example.telemetry.processor.repository.SensorReadingRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class TelemetryConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryConsumer.class);
    private static final String ALERTS_TOPIC = "sensor.alerts";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final SensorReadingRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.alerts.temperature-threshold:35.0}")
    private double temperatureThreshold;

    public TelemetryConsumer(SensorReadingRepository repository,
                             KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Main consumer: persists readings and publishes alerts when temperature exceeds threshold.
     * Extracts trace ID from Kafka headers for end-to-end correlation.
     */
    @KafkaListener(topics = "sensor.data")
    public void consume(ConsumerRecord<String, SensorReading> record) {
        String traceId = extractTraceId(record);
        MDC.put("traceId", traceId);
        try {
            SensorReading reading = record.value();

            log.info("[CONSUME] traceId={} | Received: sensor={} partition={} offset={} temp={}C humidity={}%",
                    traceId, reading.getSensorId(), record.partition(), record.offset(),
                    reading.getTemperature(), reading.getHumidity());

            // Persist to PostgreSQL
            SensorReadingEntity entity = new SensorReadingEntity(
                    reading.getSensorId().toString(),
                    reading.getTemperature(),
                    reading.getHumidity(),
                    reading.getTimestamp(),
                    reading.getLocation().toString(),
                    record.partition(),
                    record.offset()
            );
            repository.save(entity);
            log.info("[PERSIST] traceId={} | Saved to DB: id={} sensor={} location={}",
                    traceId, entity.getId(), entity.getSensorId(), entity.getLocation());

            // Check temperature threshold and publish alert
            if (reading.getTemperature() > temperatureThreshold) {
                log.warn("[ALERT] traceId={} | Temperature threshold exceeded! sensor={} temp={}C (threshold={}C)",
                        traceId, reading.getSensorId(), reading.getTemperature(), temperatureThreshold);
                kafkaTemplate.send(ALERTS_TOPIC, reading.getSensorId().toString(), reading);
                log.info("[ALERT] traceId={} | Published to {} for sensor={}",
                        traceId, ALERTS_TOPIC, reading.getSensorId());
            }
        } finally {
            MDC.remove("traceId");
        }
    }

    private String extractTraceId(ConsumerRecord<String, SensorReading> record) {
        Header header = record.headers().lastHeader(TRACE_ID_HEADER);
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return "no-trace-" + record.offset();
    }
}
