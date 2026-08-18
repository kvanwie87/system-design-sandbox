package com.example.common.dto;

import com.example.common.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderDTO(
        String id,
        String userId,
        OrderStatus status,
        List<OrderItemDTO> items,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal shippingFee,
        BigDecimal total,
        String shippingAddressId,
        String paymentId,
        String createdAt,
        String updatedAt
) {}
