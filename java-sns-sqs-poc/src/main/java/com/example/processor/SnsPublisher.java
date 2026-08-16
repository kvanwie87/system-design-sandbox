package com.example.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * Publishes a {@link CsvSummary} as a JSON message to an SNS topic.
 */
public class SnsPublisher {

    private final SnsClient snsClient;
    private final String topicArn;
    private final ObjectMapper objectMapper;

    public SnsPublisher(SnsClient snsClient, String topicArn) {
        this.snsClient = snsClient;
        this.topicArn = topicArn;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Serializes the summary to JSON and publishes it to the configured SNS topic.
     *
     * @param summary the CSV summary to publish
     * @return the SNS message ID from the publish response
     */
    public String publishSummary(CsvSummary summary) {
        String json = toJson(summary);

        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .message(json)
                .subject("CSV Processed: " + summary.getFileName())
                .build();

        PublishResponse response = snsClient.publish(request);
        return response.messageId();
    }

    String toJson(CsvSummary summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize CsvSummary to JSON", e);
        }
    }
}
