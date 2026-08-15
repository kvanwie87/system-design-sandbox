package com.example.telemetry.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

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
class SensorIngestApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the application context loads successfully
    }
}
