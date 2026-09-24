package com.kairos.kairosapipostgres.exception;

public class ProductShelfAlreadyExistsException extends RuntimeException {
    public ProductShelfAlreadyExistsException(String message) {
        super(message);
    }
}
