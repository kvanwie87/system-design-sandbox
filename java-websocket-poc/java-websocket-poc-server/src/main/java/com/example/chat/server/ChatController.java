package com.example.chat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final SessionRegistry sessionRegistry;

    public ChatController(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @MessageMapping("join")
    @SendTo("/topic/chat")
    public String handleJoin(String username, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        sessionRegistry.register(sessionId, username);
        log.info("User '{}' joined (session={})", username, sessionId);
        return username + " has joined the chat.";
    }

    @MessageMapping("chat")
    @SendTo("/topic/chat")
    public String handleMessage(String message) {
        log.info("Message received: {}", message);
        return message;
    }
}
