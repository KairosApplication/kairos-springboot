package com.kairos.kairosapipostgres.exception;

public class EmployeeAisleAlreadyExistsException extends RuntimeException {
    public EmployeeAisleAlreadyExistsException(String message) {
        super(message);
    }
}
