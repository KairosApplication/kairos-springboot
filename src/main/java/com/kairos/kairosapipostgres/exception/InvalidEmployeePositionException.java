package com.kairos.kairosapipostgres.exception;

public class InvalidEmployeePositionException extends RuntimeException {
    public InvalidEmployeePositionException (String s) {
        super (s);
    }
}
