package com.kairos.kairosapipostgres.dto.response;

public record CustomerResponse(
        Long id,
        Long userId,
        String userName
) {
}
