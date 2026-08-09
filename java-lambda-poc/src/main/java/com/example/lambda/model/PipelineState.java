package com.example.lambda.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Shared state object passed between Step Function steps.
 * Each step reads what it needs and adds its output for the next step.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PipelineState {

    @JsonProperty("sourceBucket")
    private String sourceBucket;

    @JsonProperty("sourceKey")
    private String sourceKey;

    @JsonProperty("outputBucket")
    private String outputBucket;

    @JsonProperty("outputKey")
    private String outputKey;

    @JsonProperty("headers")
    private List<String> headers;

    @JsonProperty("rows")
    private List<Map<String, String>> rows;

    @JsonProperty("status")
    private String status;

    @JsonProperty("error")
    private String error;

    public PipelineState() {
    }

    // Getters and setters

    public String getSourceBucket() {
        return sourceBucket;
    }

    public void setSourceBucket(String sourceBucket) {
        this.sourceBucket = sourceBucket;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public String getOutputBucket() {
        return outputBucket;
    }

    public void setOutputBucket(String outputBucket) {
        this.outputBucket = outputBucket;
    }

    public String getOutputKey() {
        return outputKey;
    }

    public void setOutputKey(String outputKey) {
        this.outputKey = outputKey;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<Map<String, String>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, String>> rows) {
        this.rows = rows;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
