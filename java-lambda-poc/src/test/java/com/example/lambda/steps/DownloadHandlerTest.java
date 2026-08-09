package com.example.lambda.steps;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.example.lambda.model.PipelineState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DownloadHandlerTest {

    @Mock
    private AmazonS3 mockS3;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    private DownloadHandler handler;

    @BeforeEach
    void setUp() {
        when(mockContext.getLogger()).thenReturn(mockLogger);
        handler = new DownloadHandler(mockS3);
    }

    @Test
    void handleRequest_parsesCSVIntoRows() {
        String csv = "id,product,quantity,status,price\n1,Widget,10,active,5.00\n2,Gadget,3,inactive,12.00\n";
        mockS3Object("bucket", "file.csv", csv);

        PipelineState input = new PipelineState();
        input.setSourceBucket("bucket");
        input.setSourceKey("file.csv");

        PipelineState result = handler.handleRequest(input, mockContext);

        assertEquals("DOWNLOADED", result.getStatus());
        assertEquals(5, result.getHeaders().size());
        assertEquals(2, result.getRows().size());
        assertEquals("Widget", result.getRows().get(0).get("product"));
        assertEquals("Gadget", result.getRows().get(1).get("product"));
    }

    @Test
    void handleRequest_emptyFile_returnsEmptyStatus() {
        mockS3Object("bucket", "empty.csv", "");

        PipelineState input = new PipelineState();
        input.setSourceBucket("bucket");
        input.setSourceKey("empty.csv");

        PipelineState result = handler.handleRequest(input, mockContext);

        assertEquals("EMPTY", result.getStatus());
        assertTrue(result.getRows().isEmpty());
    }

    @Test
    void handleRequest_s3Error_returnsErrorStatus() {
        when(mockS3.getObject("bucket", "missing.csv"))
                .thenThrow(new RuntimeException("NoSuchKey"));

        PipelineState input = new PipelineState();
        input.setSourceBucket("bucket");
        input.setSourceKey("missing.csv");

        PipelineState result = handler.handleRequest(input, mockContext);

        assertEquals("ERROR", result.getStatus());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("NoSuchKey"));
    }

    @Test
    void handleRequest_skipsMalformedRows() {
        String csv = "id,product,quantity,status,price\n1,Widget,10,active,5.00\n2,Short\n3,Gadget,7,active,8.00\n";
        mockS3Object("bucket", "file.csv", csv);

        PipelineState input = new PipelineState();
        input.setSourceBucket("bucket");
        input.setSourceKey("file.csv");

        PipelineState result = handler.handleRequest(input, mockContext);

        assertEquals(2, result.getRows().size());
        assertEquals("1", result.getRows().get(0).get("id"));
        assertEquals("3", result.getRows().get(1).get("id"));
    }

    private void mockS3Object(String bucket, String key, String content) {
        S3Object obj = new S3Object();
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        obj.setObjectContent(new S3ObjectInputStream(new ByteArrayInputStream(bytes), null));
        when(mockS3.getObject(bucket, key)).thenReturn(obj);
    }
}
