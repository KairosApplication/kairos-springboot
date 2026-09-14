package com.kairos.kairosapipostgres.dto.response;

public record UserResponse(
        Long id,
        String name,
        String email,
        String cpf
) {}
