package com.kairos.kairosapipostgres.exception;

public class InvalidPromotionPeriodException extends RuntimeException {
    public InvalidPromotionPeriodException(String message) {
        super(message);
    }
}
