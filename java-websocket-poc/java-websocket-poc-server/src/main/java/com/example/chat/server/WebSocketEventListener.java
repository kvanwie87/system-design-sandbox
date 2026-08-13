package com.example.chat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRegistry sessionRegistry;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate, SessionRegistry sessionRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        log.info("Client connected: session={}", event.getMessage().getHeaders().get("simpSessionId"));
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String username = sessionRegistry.unregister(sessionId);
        if (username != null) {
            log.info("User '{}' disconnected (session={})", username, sessionId);
            messagingTemplate.convertAndSend("/topic/chat", username + " has disconnected.");
        } else {
            log.info("Client disconnected: session={}", sessionId);
        }
    }
}
