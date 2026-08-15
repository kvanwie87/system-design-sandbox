package com.example.telemetry.processor.config;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Logs partition assignments and revocations to demonstrate
 * how Kafka distributes partitions across consumer instances.
 */
public class PartitionAssignmentLogger implements ConsumerRebalanceListener {

    private static final Logger log = LoggerFactory.getLogger(PartitionAssignmentLogger.class);

    private final String consumerGroup;

    public PartitionAssignmentLogger(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        log.info("[{}] Partitions ASSIGNED: {}", consumerGroup,
                partitions.stream()
                        .map(tp -> tp.topic() + "-" + tp.partition())
                        .toList());
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        log.info("[{}] Partitions REVOKED: {}", consumerGroup,
                partitions.stream()
                        .map(tp -> tp.topic() + "-" + tp.partition())
                        .toList());
    }
}
