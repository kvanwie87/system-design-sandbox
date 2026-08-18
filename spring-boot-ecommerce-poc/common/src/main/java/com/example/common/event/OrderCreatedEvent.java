package com.example.common.event;

import com.example.common.dto.OrderItemDTO;
import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEvent(
        String orderId,
        String userId,
        List<OrderItemDTO> items,
        BigDecimal total,
        String createdAt
) {}
