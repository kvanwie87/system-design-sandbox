package com.example.processor;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sns.SnsClient;

import java.io.IOException;
import java.net.URI;

/**
 * Lambda handler triggered by S3 ObjectCreated events.
 * Downloads the CSV file, parses it, and publishes a summary to SNS.
 */
public class CsvProcessorHandler implements RequestHandler<S3Event, String> {

    private final S3Client s3Client;
    private final SnsPublisher snsPublisher;
    private final CsvParser csvParser;

    /**
     * Default constructor used by Lambda runtime.
     * Builds clients with optional endpoint override for LocalStack compatibility.
     */
    public CsvProcessorHandler() {
        this.s3Client = buildS3Client();
        SnsClient snsClient = buildSnsClient();
        String topicArn = System.getenv("SNS_TOPIC_ARN");
        this.snsPublisher = new SnsPublisher(snsClient, topicArn);
        this.csvParser = new CsvParser();
    }

    /**
     * Constructor for testing — accepts pre-built dependencies.
     */
    CsvProcessorHandler(S3Client s3Client, SnsPublisher snsPublisher, CsvParser csvParser) {
        this.s3Client = s3Client;
        this.snsPublisher = snsPublisher;
        this.csvParser = csvParser;
    }

    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        LambdaLogger logger = context.getLogger();

        for (S3EventNotification.S3EventNotificationRecord record : s3Event.getRecords()) {
            String bucket = record.getS3().getBucket().getName();
            String key = record.getS3().getObject().getUrlDecodedKey();

            logger.log("Processing s3://" + bucket + "/" + key);

            try {
                CsvSummary summary = downloadAndParse(bucket, key);
                String messageId = snsPublisher.publishSummary(summary);
                logger.log("Published summary to SNS, messageId=" + messageId);
            } catch (IOException e) {
                logger.log("ERROR processing " + key + ": " + e.getMessage());
                throw new RuntimeException("Failed to process CSV file: " + key, e);
            }
        }

        return "OK";
    }

    private CsvSummary downloadAndParse(String bucket, String key) throws IOException {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> objectStream = s3Client.getObject(getRequest)) {
            return csvParser.parse(objectStream, key);
        }
    }

    private static S3Client buildS3Client() {
        String endpoint = System.getenv("AWS_ENDPOINT_URL");
        var builder = S3Client.builder()
                .forcePathStyle(true);

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    private static SnsClient buildSnsClient() {
        String endpoint = System.getenv("AWS_ENDPOINT_URL");
        var builder = SnsClient.builder();

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
