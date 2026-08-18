package com.example.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String code,
        String message,
        List<FieldError> fieldErrors,
        String timestamp
) {
    public ApiError(int status, String code, String message) {
        this(status, code, message, null, Instant.now().toString());
    }

    public ApiError(int status, String code, String message, List<FieldError> fieldErrors) {
        this(status, code, message, fieldErrors, Instant.now().toString());
    }

    public record FieldError(
            String field,
            String message,
            Object rejectedValue
    ) {}
}
