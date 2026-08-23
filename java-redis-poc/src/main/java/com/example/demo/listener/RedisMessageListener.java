package com.example.demo.listener;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.example.demo.service.PubSubService;

@Component
public class RedisMessageListener implements MessageListener {

    private final PubSubService pubSubService;

    public RedisMessageListener(@Lazy PubSubService pubSubService) {
        this.pubSubService = pubSubService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());
        pubSubService.storeMessage(channel, body);
    }
}
