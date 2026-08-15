package com.example.customthreading.model;

/**
 * Simple model representing the status of an asynchronous task.
 * Used as the value in the in-memory ConcurrentHashMap task store.
 *
 * Lifecycle: PENDING → COMPLETE (once the async work finishes and writes the result).
 */
public class TaskStatus {

    public enum Status {
        PENDING, COMPLETE
    }

    private Status status;
    private String result;

    public TaskStatus(Status status, String result) {
        this.status = status;
        this.result = result;
    }

    public static TaskStatus pending() {
        return new TaskStatus(Status.PENDING, null);
    }

    public static TaskStatus complete(String result) {
        return new TaskStatus(Status.COMPLETE, result);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
