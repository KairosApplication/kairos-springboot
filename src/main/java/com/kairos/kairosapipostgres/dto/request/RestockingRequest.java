package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record RestockingRequest(
        @NotNull @Positive Long inventoryId,
        @NotNull @Positive Long employeeId,
        @NotNull @Positive Long shelfId,
        @NotNull @Positive Long productId,
        @NotNull @Positive Integer restockedQuantity,
        @PositiveOrZero Integer damagedQuantity
) {
}
