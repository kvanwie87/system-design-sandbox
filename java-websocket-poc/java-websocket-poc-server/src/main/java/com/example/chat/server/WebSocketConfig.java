package com.example.chat.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures WebSocket STOMP messaging for the chat server.
 *
 * - Enables an in-memory message broker that routes messages to subscribers on "/topic" destinations.
 * - Sets "/app" as the prefix for messages bound for @MessageMapping controller methods.
 * - Registers "/ws" as the STOMP handshake endpoint that clients connect to.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable a simple in-memory broker for broadcasting messages to subscribers on "/topic/*"
        registry.enableSimpleBroker("/topic");

        // Messages sent by clients to destinations starting with "/app" will be routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Clients initiate the WebSocket handshake at this URL: ws://host:port/ws
        registry.addEndpoint("/ws");
    }
}
