package com.example.processor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvProcessorHandlerTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private SnsPublisher snsPublisher;

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    private CsvProcessorHandler handler;

    @BeforeEach
    void setUp() {
        when(context.getLogger()).thenReturn(logger);
        handler = new CsvProcessorHandler(s3Client, snsPublisher, new CsvParser());
    }

    @Test
    void processesS3EventDownloadsAndPublishes() {
        // Arrange: create an S3Event with one record
        S3Event s3Event = createS3Event("csv-input-bucket", "data/people.csv");

        String csvContent = "name,email,age\nAlice,alice@test.com,30\nBob,bob@test.com,25\n";
        mockS3GetObject(csvContent);
        when(snsPublisher.publishSummary(any(CsvSummary.class))).thenReturn("msg-001");

        // Act
        String result = handler.handleRequest(s3Event, context);

        // Assert
        assertEquals("OK", result);

        // Verify S3 was called with correct bucket/key
        ArgumentCaptor<GetObjectRequest> s3Captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(s3Captor.capture());
        assertEquals("csv-input-bucket", s3Captor.getValue().bucket());
        assertEquals("data/people.csv", s3Captor.getValue().key());

        // Verify SNS was called with correct summary
        ArgumentCaptor<CsvSummary> snsCaptor = ArgumentCaptor.forClass(CsvSummary.class);
        verify(snsPublisher).publishSummary(snsCaptor.capture());
        CsvSummary summary = snsCaptor.getValue();
        assertEquals("data/people.csv", summary.getFileName());
        assertEquals(2, summary.getRowCount());
        assertEquals(List.of("name", "email", "age"), summary.getColumnNames());
    }

    @Test
    void handlesMultipleRecordsInSingleEvent() {
        S3Event s3Event = createS3Event("bucket", "file1.csv", "file2.csv");

        String csv1 = "a,b\n1,2\n";
        String csv2 = "x,y\n3,4\n5,6\n";

        // Return different content for each call
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(mockResponseStream(csv1))
                .thenReturn(mockResponseStream(csv2));
        when(snsPublisher.publishSummary(any(CsvSummary.class))).thenReturn("msg-id");

        String result = handler.handleRequest(s3Event, context);

        assertEquals("OK", result);
        verify(s3Client, times(2)).getObject(any(GetObjectRequest.class));
        verify(snsPublisher, times(2)).publishSummary(any(CsvSummary.class));
    }

    @Test
    void throwsRuntimeExceptionOnS3Error() {
        S3Event s3Event = createS3Event("bucket", "bad.csv");

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException("S3 connection failed"));

        assertThrows(RuntimeException.class, () -> handler.handleRequest(s3Event, context));
    }

    // --- Helper methods ---

    private void mockS3GetObject(String content) {
        ResponseInputStream<GetObjectResponse> responseStream = mockResponseStream(content);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);
    }

    private ResponseInputStream<GetObjectResponse> mockResponseStream(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        AbortableInputStream abortableStream = AbortableInputStream.create(new ByteArrayInputStream(bytes));
        return new ResponseInputStream<>(GetObjectResponse.builder().build(), abortableStream);
    }

    private S3Event createS3Event(String bucket, String... keys) {
        List<S3EventNotification.S3EventNotificationRecord> records = new java.util.ArrayList<>();
        for (String key : keys) {
            S3EventNotification.S3Entity s3Entity = new S3EventNotification.S3Entity(
                    null,
                    new S3EventNotification.S3BucketEntity(bucket, null, null),
                    new S3EventNotification.S3ObjectEntity(key, 1024L, null, null, null),
                    null
            );
            S3EventNotification.S3EventNotificationRecord record =
                    new S3EventNotification.S3EventNotificationRecord(
                            "us-east-1",
                            "ObjectCreated:Put",
                            "aws:s3",
                            null,
                            "2.1",
                            null,
                            null,
                            s3Entity,
                            null
                    );
            records.add(record);
        }
        return new S3Event(records);
    }
}
