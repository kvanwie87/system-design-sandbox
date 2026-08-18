package com.example.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CartDTO(
        String userId,
        List<CartItemDTO> items,
        BigDecimal subtotal,
        int itemCount
) {}
