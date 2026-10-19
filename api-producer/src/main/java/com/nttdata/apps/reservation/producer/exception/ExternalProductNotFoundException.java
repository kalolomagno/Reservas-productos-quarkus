package com.nttdata.apps.reservation.producer.exception;

public class ExternalProductNotFoundException extends RuntimeException {

    public ExternalProductNotFoundException(Integer productId) {
        super("External product with ID %d was not found.".formatted(productId));
    }
}
