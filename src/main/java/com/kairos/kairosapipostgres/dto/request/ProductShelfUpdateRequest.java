package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.Positive;

public record ProductShelfUpdateRequest(
        @Positive(message = "O produto deve ter um ID positivo") Long productId,
        @Positive(message = "A prateleira deve ter um ID positivo") Long shelfId,
        @Positive(message = "A quantidade deve ser positiva") Integer productQuantity
) {
}
