package com.nttdata.apps.reservation.producer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReservationRequest(

        @NotBlank(message = "Customer name is required.")
        String customerName,

        @NotNull(message = "External product ID is required.")
        @Positive(message = "External product ID must be greater than zero.")
        Integer externalProductId,

        @NotNull(message = "Quantity is required.")
        @Positive(message = "Quantity must be greater than zero.")
        Integer quantity
) {
}
