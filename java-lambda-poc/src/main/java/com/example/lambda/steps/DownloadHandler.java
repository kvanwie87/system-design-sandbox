package com.example.lambda.steps;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.example.lambda.model.PipelineState;
import com.example.lambda.util.S3ClientFactory;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Step 1: Downloads the CSV from S3 and parses it into rows.
 *
 * Input: PipelineState with sourceBucket and sourceKey set
 * Output: PipelineState with headers and rows populated
 */
public class DownloadHandler implements RequestHandler<PipelineState, PipelineState> {

    private final AmazonS3 s3Client;

    public DownloadHandler() {
        this.s3Client = S3ClientFactory.create();
    }

    public DownloadHandler(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public PipelineState handleRequest(PipelineState state, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log(String.format("DownloadHandler: Reading s3://%s/%s%n",
                state.getSourceBucket(), state.getSourceKey()));

        try {
            S3Object s3Object = s3Client.getObject(state.getSourceBucket(), state.getSourceKey());

            try (CSVReader reader = new CSVReaderBuilder(
                    new InputStreamReader(s3Object.getObjectContent(), StandardCharsets.UTF_8)).build()) {

                String[] headerArray = reader.readNext();
                if (headerArray == null || headerArray.length == 0) {
                    state.setHeaders(Collections.emptyList());
                    state.setRows(Collections.emptyList());
                    state.setStatus("EMPTY");
                    return state;
                }

                // Trim headers
                List<String> headers = new ArrayList<>();
                for (String h : headerArray) {
                    headers.add(h.trim());
                }
                state.setHeaders(headers);

                // Parse all rows into maps
                List<Map<String, String>> rows = new ArrayList<>();
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length < headers.size()) {
                        continue; // Skip malformed rows
                    }
                    Map<String, String> row = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        row.put(headers.get(i), line[i].trim());
                    }
                    rows.add(row);
                }

                state.setRows(rows);
                state.setStatus("DOWNLOADED");
                logger.log(String.format("DownloadHandler: Parsed %d rows%n", rows.size()));
            }

        } catch (Exception e) {
            state.setStatus("ERROR");
            state.setError("Download failed: " + e.getMessage());
            logger.log("DownloadHandler ERROR: " + e.getMessage() + "\n");
        }

        return state;
    }
}
