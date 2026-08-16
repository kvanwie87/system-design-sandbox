package com.example.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnsPublisherTest {

    private static final String TOPIC_ARN = "arn:aws:sns:us-east-1:000000000000:csv-processed";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SnsClient snsClient;

    private SnsPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SnsPublisher(snsClient, TOPIC_ARN);
    }

    @Test
    void publishesSummaryAsJsonToCorrectTopic() throws Exception {
        CsvSummary summary = new CsvSummary(
                "test.csv",
                5,
                List.of("name", "email", "age"),
                List.of(Map.of("name", "Alice", "email", "alice@example.com", "age", "30"))
        );

        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("msg-123").build());

        String messageId = publisher.publishSummary(summary);

        assertEquals("msg-123", messageId);

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());

        PublishRequest captured = captor.getValue();
        assertEquals(TOPIC_ARN, captured.topicArn());
        assertEquals("CSV Processed: test.csv", captured.subject());

        // Verify the JSON payload structure
        JsonNode json = MAPPER.readTree(captured.message());
        assertEquals("test.csv", json.get("fileName").asText());
        assertEquals(5, json.get("rowCount").asInt());
        assertEquals(3, json.get("columnNames").size());
        assertEquals("name", json.get("columnNames").get(0).asText());
        assertEquals(1, json.get("firstRows").size());
        assertEquals("Alice", json.get("firstRows").get(0).get("name").asText());
    }

    @Test
    void serializesEmptySummary() {
        CsvSummary summary = new CsvSummary("empty.csv", 0, List.of(), List.of());

        String json = publisher.toJson(summary);

        assertNotNull(json);
        assertTrue(json.contains("\"fileName\":\"empty.csv\""));
        assertTrue(json.contains("\"rowCount\":0"));
        assertTrue(json.contains("\"columnNames\":[]"));
        assertTrue(json.contains("\"firstRows\":[]"));
    }

    @Test
    void returnsMessageIdFromResponse() {
        CsvSummary summary = new CsvSummary("data.csv", 10, List.of("a"), List.of());

        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("abc-456-def").build());

        String messageId = publisher.publishSummary(summary);

        assertEquals("abc-456-def", messageId);
    }
}
