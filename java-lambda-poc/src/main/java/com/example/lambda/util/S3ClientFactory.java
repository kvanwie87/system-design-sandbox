package com.example.lambda.util;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

/**
 * Factory for creating S3 clients that work both in LocalStack and real AWS.
 */
public class S3ClientFactory {

    private S3ClientFactory() {
    }

    public static AmazonS3 create() {
        String localstackHost = System.getenv("LOCALSTACK_HOSTNAME");
        String endpoint = System.getenv("AWS_ENDPOINT_URL");
        String region = System.getenv("AWS_REGION");
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }

        if (localstackHost != null && !localstackHost.isBlank()) {
            String localstackEndpoint = "http://" + localstackHost + ":4566";
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new EndpointConfiguration(localstackEndpoint, region))
                    .withPathStyleAccessEnabled(true)
                    .build();
        } else if (endpoint != null && !endpoint.isBlank()) {
            return AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new EndpointConfiguration(endpoint, region))
                    .withPathStyleAccessEnabled(true)
                    .build();
        }

        return AmazonS3ClientBuilder.defaultClient();
    }
}
