package com.kairos.kairosapipostgres.dto.response;

public record CsrfResponse(String headerName, String token) {
}
