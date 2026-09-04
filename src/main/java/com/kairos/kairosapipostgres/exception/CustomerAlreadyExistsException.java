package com.kairos.kairosapipostgres.exception;

public class CustomerAlreadyExistsException extends RuntimeException {
    public CustomerAlreadyExistsException (String message) {
        super(message);
    }
}
