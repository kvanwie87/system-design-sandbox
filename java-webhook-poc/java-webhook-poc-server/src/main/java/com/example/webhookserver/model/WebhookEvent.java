package com.example.webhookserver.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a webhook event payload sent to registered clients.
 *
 * @param eventId   unique identifier for this event instance
 * @param eventType the type of event (e.g., "STOCK_PRICE_UPDATE")
 * @param timestamp when the event was generated
 * @param data      the stock ticker data associated with this event
 */
public record WebhookEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        StockTickerData data
) {
}
