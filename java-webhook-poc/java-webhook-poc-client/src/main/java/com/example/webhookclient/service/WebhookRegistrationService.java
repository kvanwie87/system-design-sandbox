package com.example.webhookclient.service;

import com.example.webhookclient.auth.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpHeaders;
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
    private final TokenProvider tokenProvider;

    public WebhookRegistrationService(
            @Value("${webhook.server.url}") String serverUrl,
            @Value("${server.port}") int serverPort,
            TokenProvider tokenProvider) {
        this.serverUrl = serverUrl;
        this.serverPort = serverPort;
        this.tokenProvider = tokenProvider;
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
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(registrationEndpoint)
                    .header("Content-Type", "application/json");

            // Attach a Bearer JWT when auth is enabled (spring-security / hand-rolled).
            tokenProvider.bearerToken().ifPresentOrElse(
                    token -> {
                        request.header(HttpHeaders.AUTHORIZATION, token);
                        log.info("Attaching Bearer JWT to registration request to {}", registrationEndpoint);
                    },
                    () -> log.info("Sending registration request to {} without authentication", registrationEndpoint));

            request
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
