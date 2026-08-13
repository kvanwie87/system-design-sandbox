package com.example.chat.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the chat server.
 * Starts a Spring Boot web application that hosts the WebSocket STOMP endpoint.
 */
@SpringBootApplication
public class ChatServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatServerApplication.class, args);
	}
}
