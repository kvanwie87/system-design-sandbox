package com.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StepFunctionTriggerHandlerTest {

    @Mock
    private AWSStepFunctions mockSfn;

    @Mock
    private Context mockContext;

    @Mock
    private LambdaLogger mockLogger;

    private StepFunctionTriggerHandler handler;

    @BeforeEach
    void setUp() {
        when(mockContext.getLogger()).thenReturn(mockLogger);
        handler = new StepFunctionTriggerHandler(mockSfn, "arn:aws:states:us-east-1:123:stateMachine:csv-pipeline", "csv-output-bucket");
    }

    @Test
    void handleRequest_startsStateMachineExecution() {
        S3Event event = createS3Event("csv-input-bucket", "orders.csv");
        when(mockSfn.startExecution(any(StartExecutionRequest.class)))
                .thenReturn(new StartExecutionResult().withExecutionArn("arn:exec:123"));

        String result = handler.handleRequest(event, mockContext);

        assertEquals("Success", result);

        ArgumentCaptor<StartExecutionRequest> captor = ArgumentCaptor.forClass(StartExecutionRequest.class);
        verify(mockSfn).startExecution(captor.capture());

        StartExecutionRequest request = captor.getValue();
        assertEquals("arn:aws:states:us-east-1:123:stateMachine:csv-pipeline", request.getStateMachineArn());
        assertTrue(request.getInput().contains("csv-input-bucket"));
        assertTrue(request.getInput().contains("orders.csv"));
        assertTrue(request.getInput().contains("csv-output-bucket"));
        assertTrue(request.getInput().contains("orders.json"));
    }

    @Test
    void handleRequest_missingStateMachineArn_returnsError() {
        StepFunctionTriggerHandler handlerNoArn = new StepFunctionTriggerHandler(mockSfn, null, "output-bucket");
        S3Event event = createS3Event("bucket", "file.csv");

        String result = handlerNoArn.handleRequest(event, mockContext);

        assertTrue(result.contains("Error"));
        assertTrue(result.contains("STATE_MACHINE_ARN"));
        verify(mockSfn, never()).startExecution(any());
    }

    @Test
    void handleRequest_sfnError_returnsError() {
        S3Event event = createS3Event("bucket", "file.csv");
        when(mockSfn.startExecution(any()))
                .thenThrow(new RuntimeException("StateMachineDoesNotExist"));

        String result = handler.handleRequest(event, mockContext);

        assertTrue(result.startsWith("Error:"));
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
}
