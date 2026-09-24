package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.Positive;

public record ShelfUpdateRequest(
        @Positive(message = "O corredor deve ter um ID positivo") Long aisleId,
        @Positive(message = "A capacidade máxima deve ser positiva") Integer maximumCapacity
) {
}
