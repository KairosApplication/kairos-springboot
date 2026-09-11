package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.NotNull;

public record CategoryRequest(
        @NotNull(message = "Categoria é obrigatória")
        String category
) {
}
