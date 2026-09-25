package com.kairos.kairosapipostgres.exception;

public class ShelfEmployeeAlreadyExistsException extends RuntimeException {
    public ShelfEmployeeAlreadyExistsException(String message) {
        super(message);
    }
}
