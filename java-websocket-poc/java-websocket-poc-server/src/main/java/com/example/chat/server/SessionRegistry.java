package com.example.chat.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class SessionRegistry {

    private final ConcurrentMap<String, String> sessionToUsername = new ConcurrentHashMap<>();

    public void register(String sessionId, String username) {
        sessionToUsername.put(sessionId, username);
    }

    public String unregister(String sessionId) {
        return sessionToUsername.remove(sessionId);
    }
}
