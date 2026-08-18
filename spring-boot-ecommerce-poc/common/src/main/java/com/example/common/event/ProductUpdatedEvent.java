package com.example.common.event;

import com.example.common.dto.ProductDTO;

public record ProductUpdatedEvent(
        String productId,
        String action,
        ProductDTO product,
        String updatedAt
) {}
