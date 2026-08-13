package com.example.chat.client;

import java.lang.reflect.Type;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * CLI chat client that connects to the WebSocket server via STOMP.
 *
 * Implements CommandLineRunner so the chat logic runs after Spring Boot starts.
 * Since spring.main.web-application-type=none is set in application.yaml,
 * no embedded web server is launched — this is a pure console app.
 */
@SpringBootApplication
public class ChatClientApplication implements CommandLineRunner {

    private static final String SERVER_URL = "ws://localhost:8080/ws";

    public static void main(String[] args) {
        SpringApplication.run(ChatClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // Prompt the user for a display name
        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = "Anonymous";
        }

        // Create a STOMP client over a standard Jakarta WebSocket connection (via Tyrus)
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        // Configure a StringMessageConverter so the client can send/receive plain text payloads
        stompClient.setMessageConverter(new StringMessageConverter());

        // Initiate the async connection to the server's /ws STOMP endpoint
        CompletableFuture<StompSession> future = stompClient.connectAsync(SERVER_URL, new ChatSessionHandler(username));

        // Block until the connection is established (or fails)
        StompSession session;
        try {
            session = future.get();
        } catch (Exception e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            return;
        }

        System.out.println("Connected! Type messages and press Enter to send. Type /quit to exit.");
        System.out.println("---");

        // Main input loop: read lines from stdin and send them as chat messages
        String finalUsername = username;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if ("/quit".equalsIgnoreCase(line.trim())) {
                break;
            }
            if (!line.isBlank()) {
                // Send to /app/chat — the server's @MessageMapping("chat") will broadcast it
                session.send("/app/chat", finalUsername + ": " + line);
            }
        }

        // Clean up: disconnect the STOMP session and stop the client
        session.disconnect();
        stompClient.stop();
        System.out.println("Disconnected. Goodbye!");
    }

    /**
     * Handles STOMP session lifecycle events: connection, subscription, incoming frames, and errors.
     */
    private static class ChatSessionHandler extends StompSessionHandlerAdapter {

        private final String username;

        ChatSessionHandler(String username) {
            this.username = username;
        }

        /**
         * Called once the STOMP connection is fully established.
         * Subscribes to the broadcast topic and sends a join message to register the username.
         */
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            // Subscribe to /topic/chat to receive all broadcast messages
            session.subscribe("/topic/chat", new StompSessionHandlerAdapter() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    // Tell the framework to deserialize incoming frames as String
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    // Print each incoming message to the console
                    System.out.println(payload);
                }
            });

            // Notify the server of our username so it can track who's connected
            session.send("/app/join", username);
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                    StompHeaders headers, byte[] payload, Throwable exception) {
            System.err.println("Error: " + exception.getMessage());
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("Transport error: " + exception.getMessage());
        }
    }
}
