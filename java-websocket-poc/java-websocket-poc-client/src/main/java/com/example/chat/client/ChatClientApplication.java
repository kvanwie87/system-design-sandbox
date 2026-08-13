package com.example.chat.client;

import java.lang.reflect.Type;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootApplication
public class ChatClientApplication implements CommandLineRunner {

    private static final String SERVER_URL = "ws://localhost:8080/ws";

    public static void main(String[] args) {
        SpringApplication.run(ChatClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = "Anonymous";
        }

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());
        CompletableFuture<StompSession> future = stompClient.connectAsync(SERVER_URL, new ChatSessionHandler(username));

        StompSession session;
        try {
            session = future.get();
        } catch (Exception e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            return;
        }

        System.out.println("Connected! Type messages and press Enter to send. Type /quit to exit.");
        System.out.println("---");

        String finalUsername = username;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if ("/quit".equalsIgnoreCase(line.trim())) {
                break;
            }
            if (!line.isBlank()) {
                session.send("/app/chat", finalUsername + ": " + line);
            }
        }

        session.disconnect();
        stompClient.stop();
        System.out.println("Disconnected. Goodbye!");
    }

    private static class ChatSessionHandler extends StompSessionHandlerAdapter {

        private final String username;

        ChatSessionHandler(String username) {
            this.username = username;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            session.subscribe("/topic/chat", new StompSessionHandlerAdapter() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    System.out.println(payload);
                }
            });
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
