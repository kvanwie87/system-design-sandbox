package com.example.webhookclient.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Automatically registers this client with the webhook server on application startup.
 * <p>
 * Implements {@link ApplicationRunner} so that registration happens after the
 * Spring context is fully initialized and the HTTP server is ready to receive callbacks.
 * <p>
 * The callback URL is dynamically constructed from the configured server port,
 * allowing multiple client instances on different ports.
 */
@Component
public class WebhookRegistrationService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WebhookRegistrationService.class);

    private final String serverUrl;
    private final int serverPort;
    private final RestClient restClient;

    public WebhookRegistrationService(
            @Value("${webhook.server.url}") String serverUrl,
            @Value("${server.port}") int serverPort) {
        this.serverUrl = serverUrl;
        this.serverPort = serverPort;
        this.restClient = RestClient.create();
    }

    /**
     * Called once on startup. Sends a registration request to the webhook server
     * with this client's callback URL (http://localhost:{port}/webhook/events).
     */
    @Override
    public void run(ApplicationArguments args) {
        // Build the callback URL using this instance's port
        String callbackUrl = "http://localhost:" + serverPort + "/webhook/events";
        String registrationEndpoint = serverUrl + "/api/webhooks/register";

        log.info("Registering with webhook server at {}", serverUrl);

        try {
            restClient.post()
                    .uri(registrationEndpoint)
                    .header("Content-Type", "application/json")
                    .body(Map.of("callbackUrl", callbackUrl))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully registered with webhook server. Callback URL: {}", callbackUrl);
        } catch (Exception e) {
            log.error("Failed to register with webhook server at {}: {}", serverUrl, e.getMessage());
            log.error("Make sure the webhook server is running before starting the client.");
        }
    }
}
