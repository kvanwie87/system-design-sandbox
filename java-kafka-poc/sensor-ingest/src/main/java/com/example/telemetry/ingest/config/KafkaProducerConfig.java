package com.example.telemetry.ingest.config;

import com.example.telemetry.avro.SensorReading;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${spring.kafka.producer.transaction-id-prefix}")
    private String transactionIdPrefix;

    @Bean
    public ProducerFactory<String, SensorReading> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionIdPrefix);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        DefaultKafkaProducerFactory<String, SensorReading> factory =
                new DefaultKafkaProducerFactory<>(props);
        factory.setTransactionIdPrefix(transactionIdPrefix);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, SensorReading> kafkaTemplate(
            ProducerFactory<String, SensorReading> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public KafkaTransactionManager<String, SensorReading> kafkaTransactionManager(
            ProducerFactory<String, SensorReading> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }
}
