package com.example.demo.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

@Service
public class PubSubService {

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final MessageListener messageListener;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, List<String>> messageStore = new ConcurrentHashMap<>();
    private final Set<String> subscribedChannels = ConcurrentHashMap.newKeySet();

    public PubSubService(StringRedisTemplate redisTemplate,
                         RedisMessageListenerContainer listenerContainer,
                         MessageListener messageListener,
                         ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
        this.messageListener = messageListener;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish a domain event with a payload. Adds a timestamp automatically.
     */
    public void publish(String eventType, Map<String, Object> payload) {
        subscribeToChannel(eventType);

        // Enrich the event with metadata
        Map<String, Object> event = new java.util.LinkedHashMap<>();
        event.put("eventType", eventType);
        event.put("timestamp", Instant.now().toString());
        event.put("payload", payload);

        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(eventType, json);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    public void storeMessage(String channel, String message) {
        messageStore.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(message);
    }

    public List<String> getMessages(String channel) {
        List<String> messages = messageStore.get(channel);
        return messages != null ? new ArrayList<>(messages) : Collections.emptyList();
    }

    public List<String> getChannels() {
        return new ArrayList<>(subscribedChannels);
    }

    public void clearMessages(String channel) {
        messageStore.remove(channel);
    }

    private void subscribeToChannel(String channel) {
        if (subscribedChannels.add(channel)) {
            listenerContainer.addMessageListener(messageListener, new ChannelTopic(channel));
        }
    }
}
