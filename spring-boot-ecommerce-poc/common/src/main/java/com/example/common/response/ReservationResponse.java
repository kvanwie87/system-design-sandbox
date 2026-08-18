package com.example.common.response;

import com.example.common.enums.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationResponse(
        String reservationId,
        String productId,
        int quantity,
        ReservationStatus status,
        String expiresAt
) {}
