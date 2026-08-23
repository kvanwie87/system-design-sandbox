package com.example.demo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
public class PubSubService {

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final MessageListener messageListener;
    private final ConcurrentHashMap<String, List<String>> messageStore = new ConcurrentHashMap<>();
    private final Set<String> subscribedChannels = ConcurrentHashMap.newKeySet();

    public PubSubService(StringRedisTemplate redisTemplate,
                         RedisMessageListenerContainer listenerContainer,
                         MessageListener messageListener) {
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
        this.messageListener = messageListener;
    }

    public void publish(String channel, String message) {
        subscribeToChannel(channel);
        redisTemplate.convertAndSend(channel, message);
    }

    public void storeMessage(String channel, String message) {
        messageStore.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(message);
    }

    public List<String> getMessages(String channel) {
        List<String> messages = messageStore.get(channel);
        return messages != null ? new ArrayList<>(messages) : Collections.emptyList();
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
