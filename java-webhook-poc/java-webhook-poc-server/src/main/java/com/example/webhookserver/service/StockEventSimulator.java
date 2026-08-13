package com.example.webhookserver.service;

import com.example.webhookserver.model.StockTickerData;
import com.example.webhookserver.model.WebhookEvent;
import com.example.webhookserver.util.WebhookSignatureUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Periodically generates simulated stock ticker events and dispatches them
 * to all active webhook clients.
 * <p>
 * The event generation interval is configurable via {@code webhook.event.interval}.
 * Each event contains a random stock symbol, price, and price change.
 * <p>
 * Dispatched payloads are signed with HMAC-SHA256 so clients can verify authenticity.
 * Delivery failures are recorded in the {@link WebhookRegistry} to drive the
 * circuit breaker state machine.
 */
@Component
public class StockEventSimulator {

    private static final Logger log = LoggerFactory.getLogger(StockEventSimulator.class);

    private static final List<String> SYMBOLS = List.of("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA");

    private final WebhookRegistry webhookRegistry;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;
    private final Random random = new Random();

    public StockEventSimulator(
            WebhookRegistry webhookRegistry,
            ObjectMapper objectMapper,
            @Value("${webhook.secret}") String webhookSecret) {
        this.webhookRegistry = webhookRegistry;
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    /**
     * Generates a random stock price update event and broadcasts it to all
     * clients in ACTIVE or DEGRADED state.
     */
    @Scheduled(fixedDelayString = "${webhook.event.interval:45000}")
    public void simulateStockEvent() {
        // Pick a random stock symbol
        String symbol = SYMBOLS.get(random.nextInt(SYMBOLS.size()));

        // Generate random price between 100 and 500
        double price = 100 + (random.nextDouble() * 400);

        // Generate random change between -5 and +5
        double change = -5 + (random.nextDouble() * 10);

        // Calculate percent change relative to price
        double percentChange = (change / price) * 100;

        // Round to 2 decimal places for clean output
        price = Math.round(price * 100.0) / 100.0;
        change = Math.round(change * 100.0) / 100.0;
        percentChange = Math.round(percentChange * 100.0) / 100.0;

        // Build the event payload
        StockTickerData data = new StockTickerData(symbol, price, change, percentChange);
        WebhookEvent event = new WebhookEvent(UUID.randomUUID(), "STOCK_PRICE_UPDATE", Instant.now(), data);

        // Only dispatch to ACTIVE and DEGRADED clients (not SUSPENDED or REMOVED)
        List<String> clients = webhookRegistry.getActiveCallbackUrls();
        log.info("Generated event STOCK_PRICE_UPDATE for {} @ ${} ({}{}, {}%), dispatching to {} client(s)",
                symbol, price,
                change >= 0 ? "+" : "", change, percentChange,
                clients.size());

        for (String callbackUrl : clients) {
            dispatchEvent(callbackUrl, event);
        }
    }

    /**
     * Dispatches a signed event to a single client.
     * On success, records a successful delivery (may promote DEGRADED → ACTIVE).
     * On failure, records the failure (may trigger ACTIVE → DEGRADED transition).
     */
    private void dispatchEvent(String callbackUrl, WebhookEvent event) {
        try {
            // Serialize event to JSON for signing
            String payload = objectMapper.writeValueAsString(event);

            // Compute HMAC-SHA256 signature over the JSON payload
            String signature = WebhookSignatureUtils.computeSignature(webhookSecret, payload);

            // POST the signed payload to the client's callback URL
            restClient.post()
                    .uri(callbackUrl)
                    .header("Content-Type", "application/json")
                    .header(WebhookSignatureUtils.SIGNATURE_HEADER, signature)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            // Successful delivery — reset failure count, may recover from DEGRADED
            webhookRegistry.recordSuccess(callbackUrl);
            log.debug("Successfully dispatched event {} to {}", event.eventId(), callbackUrl);
        } catch (Exception e) {
            // Failed delivery — increment failure count, may trigger state transition
            log.warn("Failed to dispatch event {} to {}: {}", event.eventId(), callbackUrl, e.getMessage());
            webhookRegistry.recordFailure(callbackUrl);
        }
    }
}
