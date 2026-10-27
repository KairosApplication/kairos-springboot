package com.kairos.kairosapipostgres.exception;

public class InvalidRestockingException extends RuntimeException {
    public InvalidRestockingException(String message) {
        super(message);
    }
}
