package com.nttdata.apps.reservation.producer.exception;

public class InvalidClientCredentialsException extends RuntimeException {

    public InvalidClientCredentialsException() {
        super("Invalid client credentials.");
    }
}
