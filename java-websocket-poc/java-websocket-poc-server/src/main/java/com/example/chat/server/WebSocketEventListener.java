package com.example.chat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listens for WebSocket lifecycle events (connect/disconnect) and performs
 * logging and disconnect notifications.
 *
 * Spring automatically publishes these events when a STOMP session is
 * established or terminated.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    // Used to programmatically send messages to a topic (outside of a @MessageMapping)
    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRegistry sessionRegistry;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate, SessionRegistry sessionRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Fired after the STOMP CONNECTED frame is sent to the client.
     * At this point the session is fully established.
     */
    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        log.info("Client connected: session={}", event.getMessage().getHeaders().get("simpSessionId"));
    }

    /**
     * Fired when a client disconnects (gracefully or due to transport failure).
     * Looks up the username from the registry and broadcasts a disconnect message
     * so other clients know who left.
     */
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
