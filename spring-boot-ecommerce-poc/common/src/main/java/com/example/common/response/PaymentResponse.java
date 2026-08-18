package com.example.common.response;

import com.example.common.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        String paymentId,
        String orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String cardLast4,
        String message,
        String processedAt
) {}
