package com.kairos.kairosapipostgres.dto.response;

public record UserResponse(
        Long id,
        String nome,
        String email,
        String cpf
) {}
