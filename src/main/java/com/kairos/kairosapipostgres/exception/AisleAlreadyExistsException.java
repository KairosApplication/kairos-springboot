package com.kairos.kairosapipostgres.exception;

public class AisleAlreadyExistsException extends RuntimeException {
    public AisleAlreadyExistsException(String message) {
        super(message);
    }
}
