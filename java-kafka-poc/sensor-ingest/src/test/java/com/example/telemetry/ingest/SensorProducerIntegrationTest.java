package com.example.telemetry.ingest;

import com.example.telemetry.avro.SensorReading;
import com.example.telemetry.ingest.dto.SensorReadingRequest;
import com.example.telemetry.ingest.service.SensorProducerService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EmbeddedKafka(
        partitions = 3,
        topics = {"sensor.data"},
        brokerProperties = {"transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"}
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://test-registry",
        "spring.kafka.producer.transaction-id-prefix=test-tx-"
})
class SensorProducerIntegrationTest {

    @Autowired
    private SensorProducerService producerService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void shouldSendReadingToKafkaTopic() throws Exception {
        SensorReadingRequest request = new SensorReadingRequest(
                "test-sensor-001", 25.5, 60.0, "lab-room-1", null
        );

        producerService.sendReading(request, "test-trace-001").get();

        // Consume and verify
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        consumerProps.put("schema.registry.url", "mock://test-registry");
        consumerProps.put("specific.avro.reader", "true");
        consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        DefaultKafkaConsumerFactory<String, SensorReading> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, SensorReading> consumer = consumerFactory.createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "sensor.data");

        ConsumerRecords<String, SensorReading> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        assertFalse(records.isEmpty(), "Should have received at least one record");

        var record = records.iterator().next();
        assertEquals("test-sensor-001", record.key());

        consumer.close();
    }
}
