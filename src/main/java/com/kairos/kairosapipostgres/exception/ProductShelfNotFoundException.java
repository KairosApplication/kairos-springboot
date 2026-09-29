package com.kairos.kairosapipostgres.exception;

public class ProductShelfNotFoundException extends RuntimeException {
    public ProductShelfNotFoundException(String message) {
        super(message);
    }
}
