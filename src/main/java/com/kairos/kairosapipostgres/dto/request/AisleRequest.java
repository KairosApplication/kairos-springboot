package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AisleRequest(
        @NotNull(message = "O setor é obrigatório")
        @Positive(message = "O setor deve ter um ID positivo")
        Long sectorId,
        @NotNull(message = "A posição é obrigatória")
        @Positive(message = "A posição deve ser positiva")
        Integer position
) {
}
