package com.example.webhookclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Webhook Client application.
 * <p>
 * On startup, this application automatically registers its callback URL
 * with the webhook server, then listens for incoming stock ticker events
 * and logs them to the console.
 * <p>
 * Multiple instances can be run on different ports to demonstrate
 * multi-client broadcasting (e.g., --server.port=8082).
 */
@SpringBootApplication
public class WebhookClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebhookClientApplication.class, args);
	}

}
