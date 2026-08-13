package com.example.webhookserver.model;

/**
 * Request body for webhook client registration.
 *
 * @param callbackUrl the URL where the server should POST webhook events
 */
public record WebhookRegistration(
        String callbackUrl
) {
}
