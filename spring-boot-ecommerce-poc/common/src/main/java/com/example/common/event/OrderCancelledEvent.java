package com.example.common.event;

public record OrderCancelledEvent(
        String orderId,
        String userId,
        String reason,
        String cancelledAt
) {}
