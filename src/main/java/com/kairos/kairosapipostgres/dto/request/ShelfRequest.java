package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ShelfRequest(
        @NotNull(message = "O corredor é obrigatório")
        @Positive(message = "O corredor deve ter um ID positivo")
        Long aisleId,
        @NotNull(message = "A capacidade máxima é obrigatória")
        @Positive(message = "A capacidade máxima deve ser positiva")
        Integer maximumCapacity
) {
}
