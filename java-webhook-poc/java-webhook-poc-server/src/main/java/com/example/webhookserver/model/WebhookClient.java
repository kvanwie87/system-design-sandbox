package com.example.webhookserver.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a registered webhook client with its current state and failure tracking.
 * <p>
 * Thread-safe: uses volatile for state fields and AtomicInteger for failure count,
 * since multiple threads (scheduled tasks, HTTP handlers) may access this concurrently.
 */
public class WebhookClient {

    private final String callbackUrl;
    private volatile ClientState state;
    private final AtomicInteger consecutiveFailures;
    private volatile Instant stateChangedAt;

    public WebhookClient(String callbackUrl) {
        this.callbackUrl = callbackUrl;
        this.state = ClientState.ACTIVE;
        this.consecutiveFailures = new AtomicInteger(0);
        this.stateChangedAt = Instant.now();
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public ClientState getState() {
        return state;
    }

    /**
     * Transitions to a new state and records the timestamp of the transition.
     * The timestamp is used by the state machine to determine time-based escalations.
     */
    public void setState(ClientState state) {
        this.state = state;
        this.stateChangedAt = Instant.now();
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /** Increments the failure counter and returns the new value. */
    public int incrementFailures() {
        return consecutiveFailures.incrementAndGet();
    }

    /** Resets the failure counter to zero (called on successful delivery or re-registration). */
    public void resetFailures() {
        consecutiveFailures.set(0);
    }

    /** Returns the timestamp of the last state transition (used for timeout calculations). */
    public Instant getStateChangedAt() {
        return stateChangedAt;
    }
}
