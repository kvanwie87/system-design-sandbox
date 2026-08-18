package com.example.common.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReserveInventoryRequest(
        @NotBlank(message = "Product ID is required")
        String productId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,

        @NotBlank(message = "Order ID is required")
        String orderId
) {}
