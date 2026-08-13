package com.example.webhookclient.controller;

import com.example.webhookclient.model.WebhookEvent;
import com.example.webhookclient.util.WebhookSignatureUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that receives webhook events from the server.
 * <p>
 * Security: All incoming requests are verified using HMAC-SHA256 signature
 * validation before processing. Requests with missing or invalid signatures
 * are rejected with 401 Unauthorized.
 * <p>
 * Processing: Valid events are deserialized from the raw JSON body and
 * logged to the console in a human-readable format.
 */
@RestController
@RequestMapping("/webhook")
public class WebhookReceiverController {

    private static final Logger log = LoggerFactory.getLogger(WebhookReceiverController.class);

    private final String webhookSecret;
    private final ObjectMapper objectMapper;

    public WebhookReceiverController(
            @Value("${webhook.secret}") String webhookSecret,
            ObjectMapper objectMapper) {
        this.webhookSecret = webhookSecret;
        this.objectMapper = objectMapper;
    }

    /**
     * Receives a webhook event payload from the server.
     * <p>
     * Flow:
     * 1. Check for the presence of the X-Webhook-Signature header
     * 2. Verify the HMAC-SHA256 signature against the raw request body
     * 3. Deserialize the JSON payload into a WebhookEvent
     * 4. Log the stock event details to the console
     *
     * @param rawBody   the raw JSON request body (used for signature verification)
     * @param signature the HMAC-SHA256 signature from the X-Webhook-Signature header
     * @return 200 OK on success, 401 if signature is invalid, 400 if payload is malformed
     */
    @PostMapping("/events")
    public ResponseEntity<String> receiveEvent(
            @RequestBody String rawBody,
            @RequestHeader(value = WebhookSignatureUtils.SIGNATURE_HEADER, required = false) String signature) {

        // Step 1: Reject requests without a signature header
        if (signature == null || signature.isBlank()) {
            log.warn("Rejected webhook event: missing signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing signature");
        }

        // Step 2: Verify the signature using constant-time comparison
        if (!WebhookSignatureUtils.verifySignature(webhookSecret, rawBody, signature)) {
            log.warn("Rejected webhook event: invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        // Step 3 & 4: Deserialize and log the event
        try {
            WebhookEvent event = objectMapper.readValue(rawBody, WebhookEvent.class);
            log.info("[STOCK EVENT] {} ${} ({}{}, {}%) at {}",
                    event.data().symbol(),
                    event.data().price(),
                    event.data().change() >= 0 ? "+" : "",
                    event.data().change(),
                    event.data().percentChange(),
                    event.timestamp());
            return ResponseEntity.ok("Event received");
        } catch (Exception e) {
            log.error("Failed to deserialize webhook event: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid event payload");
        }
    }
}
