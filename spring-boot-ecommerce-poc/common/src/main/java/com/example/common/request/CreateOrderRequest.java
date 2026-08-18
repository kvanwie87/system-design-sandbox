package com.example.common.request;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank(message = "User ID is required")
        String userId,

        @NotBlank(message = "Shipping address ID is required")
        String shippingAddressId,

        String idempotencyKey
) {}
