package com.example.common.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank(message = "Order ID is required")
        String orderId,

        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency,

        @NotBlank(message = "Card last 4 digits are required")
        String cardLast4,

        String idempotencyKey
) {}
