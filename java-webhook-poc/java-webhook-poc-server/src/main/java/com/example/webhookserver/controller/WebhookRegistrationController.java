package com.example.webhookserver.controller;

import com.example.webhookserver.model.WebhookRegistration;
import com.example.webhookserver.service.WebhookRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for webhook client registration.
 * <p>
 * Clients call POST /api/webhooks/register with their callback URL to subscribe
 * to webhook events. Re-registering resets the client's state back to ACTIVE
 * (useful for self-healing after failures).
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookRegistrationController {

    private final WebhookRegistry webhookRegistry;

    public WebhookRegistrationController(WebhookRegistry webhookRegistry) {
        this.webhookRegistry = webhookRegistry;
    }

    /**
     * Registers a webhook callback URL. If the URL is already registered,
     * it resets the client state to ACTIVE (allowing recovery from DEGRADED/SUSPENDED).
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody WebhookRegistration registration) {
        webhookRegistry.register(registration.callbackUrl());
        return ResponseEntity.ok("Registered successfully");
    }
}
