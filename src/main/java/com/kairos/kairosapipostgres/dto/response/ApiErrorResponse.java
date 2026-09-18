package com.kairos.kairosapipostgres.dto.response;

import java.util.Map;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> validationErrors
) {
    public ApiErrorResponse {
        validationErrors = validationErrors == null ? Map.of() : Map.copyOf(validationErrors);
    }
}
