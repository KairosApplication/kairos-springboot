package com.kairos.kairosapipostgres.exception;

public class ProductAlreadyExistsException extends RuntimeException {
    public ProductAlreadyExistsException (String s) {
        super (s);
    }
}
