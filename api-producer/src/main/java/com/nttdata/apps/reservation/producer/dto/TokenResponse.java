package com.nttdata.apps.reservation.producer.dto;

public record TokenResponse(
        String accessToken,
        String tokenType,
        Long expiresInSeconds,
        String role
) {
}
