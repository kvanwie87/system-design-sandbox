package com.example.order.listener;

import com.example.common.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Simulates a notification service by consuming order events and logging
 * what would be an email/push notification in production.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("📧 NOTIFICATION: Email sent to user {} for order {}. " +
                 "Total: ${}, Items: {}",
                event.userId(),
                event.orderId(),
                event.total(),
                event.items() != null ? event.items().size() : 0);
    }
}
