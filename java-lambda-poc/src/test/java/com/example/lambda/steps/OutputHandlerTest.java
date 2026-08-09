package com.example.lambda.steps;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.lambda.model.PipelineState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutputHandlerTest {

    @Mock
    private AmazonS3 mockS3;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    private OutputHandler handler;

    @BeforeEach
    void setUp() {
        when(mockContext.getLogger()).thenReturn(mockLogger);
        handler = new OutputHandler(mockS3);
    }

    @Test
    void handleRequest_uploadsJsonToOutputBucket() {
        PipelineState state = new PipelineState();
        state.setOutputBucket("output-bucket");
        state.setOutputKey("data/orders.json");
        state.setSourceKey("data/orders.csv");
        state.setRows(List.of(
                Map.of("id", "1", "product", "Widget", "total", "50.00")
        ));

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals("COMPLETED", result.getStatus());

        ArgumentCaptor<String> bucketCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockS3).putObject(bucketCaptor.capture(), keyCaptor.capture(),
                any(InputStream.class), any(ObjectMetadata.class));

        assertEquals("output-bucket", bucketCaptor.getValue());
        assertEquals("data/orders.json", keyCaptor.getValue());
    }

    @Test
    void handleRequest_derivesOutputKeyFromSourceKey() {
        PipelineState state = new PipelineState();
        state.setOutputBucket("output-bucket");
        state.setOutputKey(null); // Not set — should derive
        state.setSourceKey("reports/monthly.csv");
        state.setRows(List.of(Map.of("id", "1")));

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals("reports/monthly.json", result.getOutputKey());
    }

    @Test
    void handleRequest_missingOutputBucket_returnsError() {
        PipelineState state = new PipelineState();
        state.setOutputBucket(null);
        state.setRows(List.of(Map.of("id", "1")));

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals("ERROR", result.getStatus());
        assertTrue(result.getError().contains("outputBucket"));
        verify(mockS3, never()).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));
    }

    @Test
    void handleRequest_s3Error_returnsErrorStatus() {
        PipelineState state = new PipelineState();
        state.setOutputBucket("output-bucket");
        state.setOutputKey("out.json");
        state.setSourceKey("in.csv");
        state.setRows(List.of(Map.of("id", "1")));

        doThrow(new RuntimeException("Access Denied"))
                .when(mockS3).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));

        PipelineState result = handler.handleRequest(state, mockContext);

        assertEquals("ERROR", result.getStatus());
        assertTrue(result.getError().contains("Access Denied"));
    }
}
