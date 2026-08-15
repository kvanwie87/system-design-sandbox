package com.example.telemetry.processor;

import com.example.telemetry.avro.SensorReading;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

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
        "spring.kafka.properties.schema.registry.url=mock://partition-test-registry",
        "spring.kafka.consumer.group-id=test-processor-group",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer",
        "spring.kafka.consumer.properties.specific.avro.reader=true",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=io.confluent.kafka.serializers.KafkaAvroSerializer",
        "spring.datasource.url=jdbc:h2:mem:partitiontestdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.alerts.temperature-threshold=35.0"
})
class PartitionAndConsumerGroupTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Test
    void sameKeyAlwaysLandsOnSamePartition() throws Exception {
        KafkaTemplate<String, SensorReading> template = createTestProducer();
        String sensorId = "partition-test-sensor";

        Set<Integer> partitions = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            SensorReading reading = SensorReading.newBuilder()
                    .setSensorId(sensorId)
                    .setTemperature(20.0 + i)
                    .setHumidity(50.0)
                    .setTimestamp(Instant.now())
                    .setLocation("test-location")
                    .build();

            SendResult<String, SensorReading> result =
                    template.send("sensor.data", sensorId, reading).get();
            partitions.add(result.getRecordMetadata().partition());
        }

        assertEquals(1, partitions.size(),
                "All messages with the same key should land on the same partition");
    }

    @Test
    void independentConsumerGroupsReceiveAllMessages() throws Exception {
        String topic = "sensor.data";
        KafkaTemplate<String, SensorReading> template = createTestProducer();
        int messageCount = 5;

        for (int i = 0; i < messageCount; i++) {
            SensorReading reading = SensorReading.newBuilder()
                    .setSensorId("cg-sensor-" + i)
                    .setTemperature(22.0)
                    .setHumidity(50.0)
                    .setTimestamp(Instant.now())
                    .setLocation("lab")
                    .build();
            template.send(topic, "cg-sensor-" + i, reading).get();
        }

        int groupACount = consumeMessages("group-A", topic);
        assertTrue(groupACount >= messageCount,
                "Consumer group A should receive all messages, got: " + groupACount);

        int groupBCount = consumeMessages("group-B", topic);
        assertTrue(groupBCount >= messageCount,
                "Consumer group B should independently receive all messages, got: " + groupBCount);
    }

    private int consumeMessages(String groupId, String topic) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(groupId, "true", embeddedKafka);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        props.put("schema.registry.url", "mock://partition-test-registry");
        props.put("specific.avro.reader", "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, SensorReading> factory =
                new DefaultKafkaConsumerFactory<>(props);
        Consumer<String, SensorReading> consumer = factory.createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic);

        ConsumerRecords<String, SensorReading> records =
                KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
        int count = records.count();
        consumer.close();
        return count;
    }

    private KafkaTemplate<String, SensorReading> createTestProducer() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "io.confluent.kafka.serializers.KafkaAvroSerializer");
        producerProps.put("schema.registry.url", "mock://partition-test-registry");

        DefaultKafkaProducerFactory<String, SensorReading> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);
        return new KafkaTemplate<>(producerFactory);
    }
}
