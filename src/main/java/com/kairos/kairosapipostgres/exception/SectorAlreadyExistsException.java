package com.kairos.kairosapipostgres.exception;

public class SectorAlreadyExistsException extends RuntimeException {
    public SectorAlreadyExistsException (String s) {
        super(s);
    }
}
