package com.example.lambda.steps;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.lambda.model.PipelineState;
import com.example.lambda.util.S3ClientFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Step 4: Converts the enriched rows to JSON and uploads to the output S3 bucket.
 *
 * Input: PipelineState with enriched rows, outputBucket, and outputKey set
 * Output: PipelineState with status = "COMPLETED"
 */
public class OutputHandler implements RequestHandler<PipelineState, PipelineState> {

    private final AmazonS3 s3Client;
    private final ObjectMapper objectMapper;

    public OutputHandler() {
        this.s3Client = S3ClientFactory.create();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public OutputHandler(AmazonS3 s3Client) {
        this.s3Client = s3Client;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public PipelineState handleRequest(PipelineState state, Context context) {
        LambdaLogger logger = context.getLogger();

        String outputBucket = state.getOutputBucket();
        String outputKey = state.getOutputKey();

        if (outputBucket == null || outputBucket.isBlank()) {
            state.setStatus("ERROR");
            state.setError("outputBucket is not set in pipeline state");
            return state;
        }

        if (outputKey == null || outputKey.isBlank()) {
            // Derive from sourceKey
            String sourceKey = state.getSourceKey();
            outputKey = sourceKey.replaceAll("\\.[^.]+$", "") + ".json";
            state.setOutputKey(outputKey);
        }

        try {
            String json = objectMapper.writeValueAsString(state.getRows());
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/json");
            metadata.setContentLength(jsonBytes.length);

            s3Client.putObject(
                    outputBucket,
                    outputKey,
                    new ByteArrayInputStream(jsonBytes),
                    metadata
            );

            state.setStatus("COMPLETED");
            logger.log(String.format("OutputHandler: Wrote s3://%s/%s (%d bytes)%n",
                    outputBucket, outputKey, jsonBytes.length));

        } catch (Exception e) {
            state.setStatus("ERROR");
            state.setError("Output failed: " + e.getMessage());
            logger.log("OutputHandler ERROR: " + e.getMessage() + "\n");
        }

        return state;
    }
}
