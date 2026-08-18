package com.example.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductDTO(
        String id,
        String name,
        String description,
        String category,
        BigDecimal price,
        String imageUrl,
        Double rating,
        Integer reviewCount,
        String sellerId,
        Map<String, Object> attributes,
        String createdAt,
        String updatedAt
) {}
