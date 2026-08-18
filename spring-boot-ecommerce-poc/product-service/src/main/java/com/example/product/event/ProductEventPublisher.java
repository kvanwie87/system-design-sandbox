package com.example.product.event;

import com.example.common.dto.ProductDTO;
import com.example.common.event.ProductUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes product lifecycle events to the product-events Kafka topic.
 * Consumed by the Search Service to keep the Elasticsearch index in sync.
 */
@Component
public class ProductEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProductEventPublisher.class);
    private static final String TOPIC = "product-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProductEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishProductCreated(ProductDTO product) {
        ProductUpdatedEvent event = new ProductUpdatedEvent(
                product.id(), "CREATE", product, Instant.now().toString());
        kafkaTemplate.send(TOPIC, product.id(), event);
        log.info("Published product CREATE event for: {}", product.id());
    }

    public void publishProductUpdated(ProductDTO product) {
        ProductUpdatedEvent event = new ProductUpdatedEvent(
                product.id(), "UPDATE", product, Instant.now().toString());
        kafkaTemplate.send(TOPIC, product.id(), event);
        log.info("Published product UPDATE event for: {}", product.id());
    }

    public void publishProductDeleted(String productId) {
        ProductUpdatedEvent event = new ProductUpdatedEvent(
                productId, "DELETE", null, Instant.now().toString());
        kafkaTemplate.send(TOPIC, productId, event);
        log.info("Published product DELETE event for: {}", productId);
    }
}
