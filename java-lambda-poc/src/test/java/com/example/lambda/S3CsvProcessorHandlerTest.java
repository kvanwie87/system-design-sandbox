package com.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3CsvProcessorHandlerTest {

    @Mock
    private AmazonS3 mockS3Client;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    private S3CsvProcessorHandler handler;
    private CsvProcessorService csvProcessorService;

    @BeforeEach
    void setUp() {
        when(mockContext.getLogger()).thenReturn(mockLogger);
        csvProcessorService = new CsvProcessorService();
        handler = new S3CsvProcessorHandler(mockS3Client, csvProcessorService, "csv-output-bucket");
    }

    @Test
    void handleRequest_processesFileAndUploadsJson() {
        // Arrange
        String csv = "id,product,quantity,status,price\n1,Widget,10,active,5.00\n";
        S3Event event = createS3Event("csv-input-bucket", "orders.csv");
        mockS3GetObject("csv-input-bucket", "orders.csv", csv);

        // Act
        String result = handler.handleRequest(event, mockContext);

        // Assert
        assertEquals("Success", result);

        ArgumentCaptor<String> bucketCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<InputStream> inputCaptor = ArgumentCaptor.forClass(InputStream.class);
        ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);

        verify(mockS3Client).putObject(
                bucketCaptor.capture(),
                keyCaptor.capture(),
                inputCaptor.capture(),
                metadataCaptor.capture()
        );

        assertEquals("csv-output-bucket", bucketCaptor.getValue());
        assertEquals("orders.json", keyCaptor.getValue());
        assertEquals("application/json", metadataCaptor.getValue().getContentType());
    }

    @Test
    void handleRequest_replacesExtensionWithJson() {
        String csv = "id,product,quantity,status,price\n1,Widget,10,active,5.00\n";
        S3Event event = createS3Event("csv-input-bucket", "data/reports/monthly.csv");
        mockS3GetObject("csv-input-bucket", "data/reports/monthly.csv", csv);

        handler.handleRequest(event, mockContext);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockS3Client).putObject(anyString(), keyCaptor.capture(), any(InputStream.class), any(ObjectMetadata.class));
        assertEquals("data/reports/monthly.json", keyCaptor.getValue());
    }

    @Test
    void handleRequest_missingOutputBucket_returnsError() {
        S3CsvProcessorHandler handlerNoOutput = new S3CsvProcessorHandler(mockS3Client, csvProcessorService, null);
        S3Event event = createS3Event("csv-input-bucket", "orders.csv");

        String result = handlerNoOutput.handleRequest(event, mockContext);

        assertTrue(result.contains("Error"));
        assertTrue(result.contains("OUTPUT_BUCKET"));
        verify(mockS3Client, never()).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));
    }

    @Test
    void handleRequest_s3Error_returnsErrorMessage() {
        S3Event event = createS3Event("csv-input-bucket", "missing.csv");
        when(mockS3Client.getObject("csv-input-bucket", "missing.csv"))
                .thenThrow(new RuntimeException("NoSuchKey: The specified key does not exist."));

        String result = handler.handleRequest(event, mockContext);

        assertTrue(result.startsWith("Error:"));
        verify(mockS3Client, never()).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));
    }

    private S3Event createS3Event(String bucket, String key) {
        S3EventNotification.S3BucketEntity bucketEntity =
                new S3EventNotification.S3BucketEntity(bucket, null, null);
        S3EventNotification.S3ObjectEntity objectEntity =
                new S3EventNotification.S3ObjectEntity(key, 100L, null, null, null);
        S3EventNotification.S3Entity s3Entity =
                new S3EventNotification.S3Entity(null, bucketEntity, objectEntity, null);

        S3EventNotification.S3EventNotificationRecord record =
                new S3EventNotification.S3EventNotificationRecord(
                        null, null, null, null, null, null, null, s3Entity, null);

        return new S3Event(List.of(record));
    }

    private void mockS3GetObject(String bucket, String key, String content) {
        S3Object s3Object = new S3Object();
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        s3Object.setObjectContent(new S3ObjectInputStream(new ByteArrayInputStream(bytes), null));
        when(mockS3Client.getObject(bucket, key)).thenReturn(s3Object);
    }
}
