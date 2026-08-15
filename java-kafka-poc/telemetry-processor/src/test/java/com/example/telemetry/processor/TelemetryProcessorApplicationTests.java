package com.example.telemetry.processor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EmbeddedKafka(
        partitions = 3,
        topics = {"sensor.data", "sensor.data.DLT", "sensor.alerts"},
        brokerProperties = {
                "transaction.state.log.replication.factor=1",
                "transaction.state.log.min.isr=1"
        }
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.properties.schema.registry.url=mock://ctx-test-registry",
        "spring.kafka.consumer.group-id=test-processor-group",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer",
        "spring.kafka.consumer.properties.specific.avro.reader=true",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=io.confluent.kafka.serializers.KafkaAvroSerializer",
        "spring.datasource.url=jdbc:h2:mem:ctxtestdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.alerts.temperature-threshold=35.0"
})
class TelemetryProcessorApplicationTests {

    @Test
    void contextLoads() {
    }
}
