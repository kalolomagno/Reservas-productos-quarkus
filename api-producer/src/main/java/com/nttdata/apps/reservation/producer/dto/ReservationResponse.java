package com.nttdata.apps.reservation.producer.dto;

import com.nttdata.apps.reservation.producer.entity.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        String customerName,
        Integer externalProductId,
        String productTitle,
        Integer quantity,
        ReservationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
