package com.nttdata.apps.reservation.producer.service;

public record ExternalProduct(
        Integer id,
        String title,
        Integer stock
) {
}
