package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CustomerRequest(
        @NotNull(message = "O usuário é obrigatório")
        Long userId,
        @NotNull(message = "A empresa é obrigatória")
        @Positive(message = "O ID da empresa deve ser positivo")
        Long companyId
) {
}
