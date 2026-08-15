package com.example.telemetry.processor.consumer;

import com.example.telemetry.avro.SensorReading;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A lightweight second consumer group that independently tracks message counts.
 * Demonstrates that multiple consumer groups each get all messages independently
 * with their own offset tracking.
 */
@Component
public class TelemetryMonitorConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryMonitorConsumer.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final ConcurrentHashMap<String, AtomicLong> sensorMessageCounts = new ConcurrentHashMap<>();
    private final AtomicLong totalMessages = new AtomicLong(0);

    @KafkaListener(
            topics = "sensor.data",
            groupId = "telemetry-monitor-group"
    )
    public void monitor(ConsumerRecord<String, SensorReading> record) {
        long count = totalMessages.incrementAndGet();
        String sensorId = record.key();
        String traceId = extractTraceId(record);
        MDC.put("traceId", traceId);
        try {
            sensorMessageCounts.computeIfAbsent(sensorId, k -> new AtomicLong(0)).incrementAndGet();

            log.info("[MONITOR] traceId={} | msg#{} sensor={} partition={} offset={} temp={}C",
                    traceId, count, sensorId, record.partition(), record.offset(),
                    record.value().getTemperature());

            if (count % 10 == 0) {
                log.info("[MONITOR] Stats: total={} | sensors tracked: {}",
                        count, sensorMessageCounts.size());
            }
        } finally {
            MDC.remove("traceId");
        }
    }

    public long getTotalMessageCount() {
        return totalMessages.get();
    }

    public long getMessageCountForSensor(String sensorId) {
        AtomicLong count = sensorMessageCounts.get(sensorId);
        return count != null ? count.get() : 0;
    }

    private String extractTraceId(ConsumerRecord<String, SensorReading> record) {
        Header header = record.headers().lastHeader(TRACE_ID_HEADER);
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return "no-trace-" + record.offset();
    }
}
