package com.kairos.kairosapipostgres.exception;

public class ShelfEmployeeNotFoundException extends RuntimeException {
    public ShelfEmployeeNotFoundException(String message) {
        super(message);
    }
}
