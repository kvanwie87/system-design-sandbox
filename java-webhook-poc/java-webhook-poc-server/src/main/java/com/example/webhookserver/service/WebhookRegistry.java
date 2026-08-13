package com.example.webhookserver.service;

import com.example.webhookserver.model.ClientState;
import com.example.webhookserver.model.WebhookClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages webhook client registrations and implements a circuit breaker state machine
 * for handling unresponsive clients.
 * <p>
 * State machine transitions:
 * <pre>
 *   ACTIVE ──(3 failures)──► DEGRADED ──(30s timeout)──► SUSPENDED ──(60s timeout)──► REMOVED
 *      ▲                         │
 *      └────(successful delivery)┘
 * </pre>
 * <p>
 * - ACTIVE: Normal operation, events are dispatched.
 * - DEGRADED: Still receives events (gives the client a chance to recover).
 *             A successful delivery promotes back to ACTIVE.
 * - SUSPENDED: No events dispatched. Client can re-register to reset.
 * - REMOVED: Permanently evicted from the registry.
 * <p>
 * Re-registration from any state resets the client back to ACTIVE.
 * <p>
 * Thread-safe: uses ConcurrentHashMap for client storage.
 */
@Service
public class WebhookRegistry {

    private static final Logger log = LoggerFactory.getLogger(WebhookRegistry.class);

    /** In-memory store of all registered clients, keyed by callback URL. */
    private final ConcurrentHashMap<String, WebhookClient> clients = new ConcurrentHashMap<>();

    /** Number of consecutive failures before transitioning from ACTIVE to DEGRADED. */
    private final int maxFailuresBeforeDegraded;

    /** Time (ms) a client stays in DEGRADED before transitioning to SUSPENDED. */
    private final long degradedToSuspendedMs;

    /** Time (ms) a client stays in SUSPENDED before being permanently REMOVED. */
    private final long suspendedToRemovedMs;

    public WebhookRegistry(
            @Value("${webhook.circuit-breaker.max-failures:3}") int maxFailuresBeforeDegraded,
            @Value("${webhook.circuit-breaker.degraded-timeout:30000}") long degradedToSuspendedMs,
            @Value("${webhook.circuit-breaker.suspended-timeout:60000}") long suspendedToRemovedMs) {
        this.maxFailuresBeforeDegraded = maxFailuresBeforeDegraded;
        this.degradedToSuspendedMs = degradedToSuspendedMs;
        this.suspendedToRemovedMs = suspendedToRemovedMs;
    }

    /**
     * Registers a client. If the client already exists (in any state), resets it back to ACTIVE.
     * This allows clients to self-heal by simply re-registering after being degraded or suspended.
     */
    public void register(String callbackUrl) {
        WebhookClient existing = clients.get(callbackUrl);
        if (existing != null) {
            existing.setState(ClientState.ACTIVE);
            existing.resetFailures();
            log.info("Webhook client re-registered (reset to ACTIVE): {}", callbackUrl);
        } else {
            clients.put(callbackUrl, new WebhookClient(callbackUrl));
            log.info("New webhook client registered: {}", callbackUrl);
        }
    }

    /**
     * Records a successful delivery — resets failures and promotes DEGRADED → ACTIVE.
     * Called by the dispatcher after a successful POST to the client.
     */
    public void recordSuccess(String callbackUrl) {
        WebhookClient client = clients.get(callbackUrl);
        if (client == null) return;

        // If the client was degraded, a successful delivery means it recovered
        if (client.getState() == ClientState.DEGRADED) {
            client.setState(ClientState.ACTIVE);
            log.info("Webhook client {} recovered, state: DEGRADED -> ACTIVE", callbackUrl);
        }
        client.resetFailures();
    }

    /**
     * Records a delivery failure. Transitions ACTIVE → DEGRADED after reaching
     * the configured max failure threshold.
     * Called by the dispatcher after a failed POST to the client.
     */
    public void recordFailure(String callbackUrl) {
        WebhookClient client = clients.get(callbackUrl);
        if (client == null) return;

        int failures = client.incrementFailures();

        // Transition to DEGRADED once failure threshold is reached
        if (client.getState() == ClientState.ACTIVE && failures >= maxFailuresBeforeDegraded) {
            client.setState(ClientState.DEGRADED);
            log.warn("Webhook client {} state: ACTIVE -> DEGRADED (after {} consecutive failures)",
                    callbackUrl, failures);
        }
    }

    /**
     * Scheduled task that checks for time-based state transitions.
     * Runs periodically (configurable via webhook.circuit-breaker.check-interval).
     * <p>
     * - DEGRADED clients that haven't recovered within the timeout → SUSPENDED
     * - SUSPENDED clients that haven't re-registered within the timeout → REMOVED
     */
    @Scheduled(fixedDelayString = "${webhook.circuit-breaker.check-interval:10000}")
    public void advanceStateTransitions() {
        Instant now = Instant.now();

        for (Map.Entry<String, WebhookClient> entry : clients.entrySet()) {
            WebhookClient client = entry.getValue();
            long elapsed = Duration.between(client.getStateChangedAt(), now).toMillis();

            switch (client.getState()) {
                case DEGRADED -> {
                    if (elapsed >= degradedToSuspendedMs) {
                        client.setState(ClientState.SUSPENDED);
                        log.warn("Webhook client {} state: DEGRADED -> SUSPENDED (no recovery after {}ms)",
                                client.getCallbackUrl(), degradedToSuspendedMs);
                    }
                }
                case SUSPENDED -> {
                    if (elapsed >= suspendedToRemovedMs) {
                        client.setState(ClientState.REMOVED);
                        clients.remove(entry.getKey());
                        log.warn("Webhook client {} state: SUSPENDED -> REMOVED (permanently evicted after {}ms)",
                                client.getCallbackUrl(), suspendedToRemovedMs);
                    }
                }
                default -> { /* ACTIVE clients are healthy; REMOVED are already gone */ }
            }
        }
    }

    /**
     * Returns callback URLs for clients in ACTIVE or DEGRADED state only.
     * DEGRADED clients still receive events to allow recovery via successful delivery.
     * SUSPENDED and REMOVED clients are excluded from dispatch.
     */
    public List<String> getActiveCallbackUrls() {
        return clients.values().stream()
                .filter(c -> c.getState() == ClientState.ACTIVE || c.getState() == ClientState.DEGRADED)
                .map(WebhookClient::getCallbackUrl)
                .toList();
    }

    /**
     * Returns all registered client URLs regardless of state (useful for monitoring/debugging).
     */
    public List<String> getCallbackUrls() {
        return List.copyOf(clients.keySet());
    }

    public int getClientCount() {
        return clients.size();
    }

    public ClientState getClientState(String callbackUrl) {
        WebhookClient client = clients.get(callbackUrl);
        return client != null ? client.getState() : null;
    }
}
