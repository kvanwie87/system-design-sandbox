package com.example.order.event;

import com.example.common.dto.OrderDTO;
import com.example.common.dto.OrderItemDTO;
import com.example.common.event.OrderCancelledEvent;
import com.example.common.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes order lifecycle events to the order-events Kafka topic.
 * Consumed by the notification listener and potentially other downstream services.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(OrderDTO order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.id(),
                order.userId(),
                order.items(),
                order.total(),
                Instant.now().toString()
        );
        kafkaTemplate.send(TOPIC, order.id(), event);
        log.info("Published OrderCreatedEvent for order: {}", order.id());
    }

    public void publishOrderCancelled(String orderId, String userId, String reason) {
        OrderCancelledEvent event = new OrderCancelledEvent(
                orderId, userId, reason, Instant.now().toString());
        kafkaTemplate.send(TOPIC, orderId, event);
        log.info("Published OrderCancelledEvent for order: {}", orderId);
    }
}
