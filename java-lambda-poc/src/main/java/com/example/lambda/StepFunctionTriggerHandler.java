package com.example.lambda;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;
import com.amazonaws.services.stepfunctions.model.StartExecutionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.lambda.model.PipelineState;

/**
 * Lambda handler that receives S3 events and starts a Step Functions execution.
 *
 * Configuration:
 * - STATE_MACHINE_ARN: ARN of the Step Functions state machine
 * - OUTPUT_BUCKET: name of the output S3 bucket
 */
public class StepFunctionTriggerHandler implements RequestHandler<S3Event, String> {

    private final AWSStepFunctions sfnClient;
    private final String stateMachineArn;
    private final String outputBucket;
    private final ObjectMapper objectMapper;

    public StepFunctionTriggerHandler() {
        this.sfnClient = buildSfnClient();
        this.stateMachineArn = System.getenv("STATE_MACHINE_ARN");
        this.outputBucket = System.getenv("OUTPUT_BUCKET");
        this.objectMapper = new ObjectMapper();
    }

    public StepFunctionTriggerHandler(AWSStepFunctions sfnClient, String stateMachineArn, String outputBucket) {
        this.sfnClient = sfnClient;
        this.stateMachineArn = stateMachineArn;
        this.outputBucket = outputBucket;
        this.objectMapper = new ObjectMapper();
    }

    private static AWSStepFunctions buildSfnClient() {
        String localstackHost = System.getenv("LOCALSTACK_HOSTNAME");
        String endpoint = System.getenv("AWS_ENDPOINT_URL");
        String region = System.getenv("AWS_REGION");
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }

        if (localstackHost != null && !localstackHost.isBlank()) {
            String localstackEndpoint = "http://" + localstackHost + ":4566";
            return AWSStepFunctionsClientBuilder.standard()
                    .withEndpointConfiguration(new EndpointConfiguration(localstackEndpoint, region))
                    .build();
        } else if (endpoint != null && !endpoint.isBlank()) {
            return AWSStepFunctionsClientBuilder.standard()
                    .withEndpointConfiguration(new EndpointConfiguration(endpoint, region))
                    .build();
        }

        return AWSStepFunctionsClientBuilder.defaultClient();
    }

    @Override
    public String handleRequest(S3Event event, Context context) {
        LambdaLogger logger = context.getLogger();

        if (stateMachineArn == null || stateMachineArn.isBlank()) {
            String error = "STATE_MACHINE_ARN environment variable is not set";
            logger.log("ERROR: " + error + "\n");
            return "Error: " + error;
        }

        for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {
            String sourceBucket = record.getS3().getBucket().getName();
            String sourceKey = record.getS3().getObject().getUrlDecodedKey();

            logger.log(String.format("Trigger: Starting state machine for s3://%s/%s%n",
                    sourceBucket, sourceKey));

            try {
                // Build initial pipeline state
                PipelineState initialState = new PipelineState();
                initialState.setSourceBucket(sourceBucket);
                initialState.setSourceKey(sourceKey);
                initialState.setOutputBucket(outputBucket);
                initialState.setOutputKey(sourceKey.replaceAll("\\.[^.]+$", "") + ".json");

                String input = objectMapper.writeValueAsString(initialState);

                StartExecutionRequest request = new StartExecutionRequest()
                        .withStateMachineArn(stateMachineArn)
                        .withInput(input);

                StartExecutionResult result = sfnClient.startExecution(request);
                logger.log(String.format("Started execution: %s%n", result.getExecutionArn()));

            } catch (Exception e) {
                logger.log(String.format("ERROR starting state machine for s3://%s/%s: %s%n",
                        sourceBucket, sourceKey, e.getMessage()));
                return "Error: " + e.getMessage();
            }
        }

        return "Success";
    }
}
