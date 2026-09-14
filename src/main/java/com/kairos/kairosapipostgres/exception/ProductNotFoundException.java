package com.kairos.kairosapipostgres.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException (String s) {
        super (s);
    }
}
