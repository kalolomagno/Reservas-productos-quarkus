package com.nttdata.apps.reservation.producer.security;

public record AuthenticatedClient(
        String clientId,
        String role
) {
}
