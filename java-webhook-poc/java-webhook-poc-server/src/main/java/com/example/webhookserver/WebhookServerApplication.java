package com.example.webhookserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Webhook Server application.
 * <p>
 * This server periodically generates simulated stock ticker events and
 * broadcasts them to all registered webhook clients. It also manages
 * client lifecycle through a circuit breaker state machine.
 * <p>
 * {@code @EnableScheduling} activates the scheduled tasks for event
 * generation and client state transition checks.
 */
@SpringBootApplication
@EnableScheduling
public class WebhookServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebhookServerApplication.class, args);
	}

}
