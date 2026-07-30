package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Entry point for running the application locally with Testcontainers.
 * Starts the app, runs the DataLoader, then shuts down.
 * Usage: ./gradlew bootTestRun
 */
public class TestDemoApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(DemoApplication.class)
				.sources(TestcontainersConfiguration.class)
				.run(args);
		// DataLoader (ApplicationRunner) has already executed by this point.
		// Trigger graceful shutdown.
		SpringApplication.exit(context, () -> 0);
	}
}
