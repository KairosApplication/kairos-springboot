package com.kairos.kairosapipostgres.exception;

public class CpfInvalidException extends RuntimeException {
    public CpfInvalidException (String message) {
        super(message);
    }
}
