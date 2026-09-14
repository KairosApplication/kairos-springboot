package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CategoryUpdateRequest(
        @NotBlank(message = "Categoria é obrigatória")
        String category
) {
}
