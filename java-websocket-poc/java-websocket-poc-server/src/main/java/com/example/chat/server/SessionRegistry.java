package com.example.chat.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

/**
 * Thread-safe registry that maps WebSocket session IDs to usernames.
 *
 * When a client sends a "join" message, the controller registers the mapping here.
 * When a client disconnects, the event listener looks up the username to broadcast
 * a disconnect notification that includes who left.
 */
@Component
public class SessionRegistry {

    // ConcurrentHashMap ensures safe access from multiple WebSocket threads
    private final ConcurrentMap<String, String> sessionToUsername = new ConcurrentHashMap<>();

    /**
     * Associates a session ID with a username.
     */
    public void register(String sessionId, String username) {
        sessionToUsername.put(sessionId, username);
    }

    /**
     * Removes and returns the username for a given session ID.
     * Returns null if the session was not registered (e.g., disconnected before joining).
     */
    public String unregister(String sessionId) {
        return sessionToUsername.remove(sessionId);
    }
}
