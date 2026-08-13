package com.example.webhookserver.model;

/**
 * Represents the lifecycle state of a webhook client in the circuit breaker state machine.
 * <p>
 * State transitions:
 * <pre>
 * ACTIVE → DEGRADED → SUSPENDED → REMOVED
 *          (failures)  (timeout)   (timeout)
 * </pre>
 * <p>
 * A client can transition back to ACTIVE from any state by re-registering.
 */
public enum ClientState {

    /** Client is healthy and receiving events normally. */
    ACTIVE,

    /** Client has hit the failure threshold but still receives events to allow recovery. */
    DEGRADED,

    /** Client is suspended — no events are dispatched. Awaiting removal or re-registration. */
    SUSPENDED,

    /** Client has been permanently removed from the registry. */
    REMOVED
}
