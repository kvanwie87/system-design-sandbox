package com.example.chat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * Handles incoming STOMP messages for the chat room.
 *
 * - /app/join  -> registers the user's session and broadcasts a join notification
 * - /app/chat  -> broadcasts the chat message to all subscribers on /topic/chat
 */
@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final SessionRegistry sessionRegistry;

    public ChatController(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Called when a client sends a message to /app/join.
     * Associates the WebSocket session ID with the provided username so we can
     * identify who disconnects later.
     *
     * @param username       the username sent by the client
     * @param headerAccessor provides access to the underlying STOMP session ID
     * @return a join notification string broadcast to /topic/chat
     */
    @MessageMapping("join")
    @SendTo("/topic/chat")
    public String handleJoin(String username, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        sessionRegistry.register(sessionId, username);
        log.info("User '{}' joined (session={})", username, sessionId);
        return username + " has joined the chat.";
    }

    /**
     * Called when a client sends a message to /app/chat.
     * Simply echoes the message back to all subscribers on /topic/chat.
     * The client pre-formats the message as "username: text".
     *
     * @param message the pre-formatted chat message
     * @return the same message, broadcast to all subscribers
     */
    @MessageMapping("chat")
    @SendTo("/topic/chat")
    public String handleMessage(String message) {
        log.info("Message received: {}", message);
        return message;
    }
}
