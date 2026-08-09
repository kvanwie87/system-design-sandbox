package com.example.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * AWS Lambda handler that triggers on S3 ObjectCreated events.
 *
 * When a CSV file is uploaded to the input bucket, this handler:
 * 1. Downloads the CSV file from S3
 * 2. Processes it (filter active rows, add total column, convert to JSON)
 * 3. Uploads the JSON result to the output bucket
 *
 * Configuration:
 * - OUTPUT_BUCKET environment variable: name of the output S3 bucket
 */
public class S3CsvProcessorHandler implements RequestHandler<S3Event, String> {

    private final AmazonS3 s3Client;
    private final CsvProcessorService csvProcessorService;
    private final String outputBucket;

    public S3CsvProcessorHandler() {
        this.s3Client = buildS3Client();
        this.csvProcessorService = new CsvProcessorService();
        this.outputBucket = System.getenv("OUTPUT_BUCKET");
    }

    private static AmazonS3 buildS3Client() {
        // Check for LocalStack: LOCALSTACK_HOSTNAME is set automatically inside Lambda containers
        String localstackHost = System.getenv("LOCALSTACK_HOSTNAME");
        String endpoint = System.getenv("AWS_ENDPOINT_URL");
        String region = System.getenv("AWS_REGION");
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }

        if (localstackHost != null && !localstackHost.isBlank()) {
            // Running inside LocalStack — use internal endpoint
            String localstackEndpoint = "http://" + localstackHost + ":4566";
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new EndpointConfiguration(localstackEndpoint, region))
                    .withPathStyleAccessEnabled(true)
                    .build();
        } else if (endpoint != null && !endpoint.isBlank()) {
            // Custom endpoint (e.g., testing outside container)
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new EndpointConfiguration(endpoint, region))
                    .withPathStyleAccessEnabled(true)
                    .build();
        }

        // Real AWS — use default client
        return AmazonS3ClientBuilder.defaultClient();
    }

    /**
     * Constructor for testing with injected dependencies.
     */
    public S3CsvProcessorHandler(AmazonS3 s3Client, CsvProcessorService csvProcessorService, String outputBucket) {
        this.s3Client = s3Client;
        this.csvProcessorService = csvProcessorService;
        this.outputBucket = outputBucket;
    }

    @Override
    public String handleRequest(S3Event event, Context context) {
        LambdaLogger logger = context.getLogger();

        if (outputBucket == null || outputBucket.isBlank()) {
            String error = "OUTPUT_BUCKET environment variable is not set";
            logger.log("ERROR: " + error + "\n");
            return "Error: " + error;
        }

        for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {
            String sourceBucket = record.getS3().getBucket().getName();
            String sourceKey = record.getS3().getObject().getUrlDecodedKey();

            logger.log(String.format("Processing file: s3://%s/%s%n", sourceBucket, sourceKey));

            try {
                // Download CSV from S3
                S3Object s3Object = s3Client.getObject(sourceBucket, sourceKey);
                InputStream csvInput = s3Object.getObjectContent();

                // Process CSV
                String jsonOutput = csvProcessorService.processCSV(csvInput);

                // Determine output key (replace .csv extension with .json)
                String outputKey = sourceKey.replaceAll("\\.[^.]+$", "") + ".json";

                // Upload JSON to output bucket
                byte[] jsonBytes = jsonOutput.getBytes(StandardCharsets.UTF_8);
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType("application/json");
                metadata.setContentLength(jsonBytes.length);

                s3Client.putObject(
                        outputBucket,
                        outputKey,
                        new ByteArrayInputStream(jsonBytes),
                        metadata
                );

                logger.log(String.format("Successfully wrote output to s3://%s/%s (%d bytes)%n",
                        outputBucket, outputKey, jsonBytes.length));

            } catch (Exception e) {
                logger.log(String.format("ERROR processing s3://%s/%s: %s%n",
                        sourceBucket, sourceKey, e.getMessage()));
                return "Error: " + e.getMessage();
            }
        }

        return "Success";
    }
}
