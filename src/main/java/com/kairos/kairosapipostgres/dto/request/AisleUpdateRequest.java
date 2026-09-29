package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.Positive;

public record AisleUpdateRequest(
        @Positive(message = "O setor deve ter um ID positivo") Long sectorId,
        @Positive(message = "A posição deve ser positiva") Integer position
) {
}
