package com.kairos.kairosapipostgres.exception;

public class RestockingNotFoundException extends RuntimeException {
    public RestockingNotFoundException(String message) {
        super(message);
    }
}
