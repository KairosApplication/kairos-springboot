package com.kairos.kairosapipostgres.exception;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException (String s) {
        super(s);
    }
}
