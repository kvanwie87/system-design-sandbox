package com.example.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InventoryDTO(
        String id,
        String productId,
        String warehouseId,
        int availableQty,
        int reservedQty,
        String updatedAt
) {}
