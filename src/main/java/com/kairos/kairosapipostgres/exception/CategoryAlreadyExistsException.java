package com.kairos.kairosapipostgres.exception;

public class CategoryAlreadyExistsException extends RuntimeException {
    public CategoryAlreadyExistsException (String s) {
        super(s);
    }
}
