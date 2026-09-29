package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductShelfRequest(
        @NotNull(message = "O produto é obrigatório")
        @Positive(message = "O produto deve ter um ID positivo") Long productId,
        @NotNull(message = "A prateleira é obrigatória")
        @Positive(message = "A prateleira deve ter um ID positivo") Long shelfId,
        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser positiva") Integer productQuantity
) {
}
