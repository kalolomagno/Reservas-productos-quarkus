package com.nttdata.apps.reservation.producer.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DummyJsonProductResponse(
        Integer id,
        String title,
        Integer stock
) {
}
