package com.kairos.kairosapipostgres.dto.response;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
