package com.nttdata.apps.reservation.producer.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(

        @NotBlank(message = "Client ID is required.")
        String clientId,

        @NotBlank(message = "Client secret is required.")
        String clientSecret
) {
}
