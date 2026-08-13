package com.example.webhookclient.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a webhook event payload received from the server.
 * Mirrors the server-side WebhookEvent record for deserialization.
 *
 * @param eventId   unique identifier for this event instance
 * @param eventType the type of event (e.g., "STOCK_PRICE_UPDATE")
 * @param timestamp when the event was generated on the server
 * @param data      the stock ticker data associated with this event
 */
public record WebhookEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        StockTickerData data
) {
}
