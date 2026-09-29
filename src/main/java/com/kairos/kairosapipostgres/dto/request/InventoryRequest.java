package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventoryRequest(
        @NotNull(message = "O setor é obrigatório")
        @Positive(message = "O setor deve ter um ID positivo") Long sectorId
) {
}
