package com.nttdata.apps.reservation.producer.exception;

import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> violations
) {

    public static ApiErrorResponse of(
            Response.Status status,
            String message
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status.getStatusCode(),
                status.getReasonPhrase(),
                message,
                Map.of()
        );
    }

    public static ApiErrorResponse of(
            Response.Status status,
            String message,
            Map<String, String> violations
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status.getStatusCode(),
                status.getReasonPhrase(),
                message,
                violations
        );
    }
}
