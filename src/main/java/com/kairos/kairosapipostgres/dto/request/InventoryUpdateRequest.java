package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.Positive;

public record InventoryUpdateRequest(
        @Positive(message = "O setor deve ter um ID positivo") Long sectorId
) {
}
