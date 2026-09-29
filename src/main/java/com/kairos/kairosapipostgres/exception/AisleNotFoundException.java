package com.kairos.kairosapipostgres.exception;

public class AisleNotFoundException extends RuntimeException {
    public AisleNotFoundException(String message) {
        super(message);
    }
}
