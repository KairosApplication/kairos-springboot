package com.kairos.kairosapipostgres.exception;

public class EmployeeAisleNotFoundException extends RuntimeException {
    public EmployeeAisleNotFoundException(String message) {
        super(message);
    }
}
