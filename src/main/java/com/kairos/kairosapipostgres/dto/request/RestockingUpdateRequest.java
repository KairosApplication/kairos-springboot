package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record RestockingUpdateRequest(
        @Positive Long inventoryId,
        @Positive Long employeeId,
        @Positive Long shelfId,
        @Positive Long productId,
        @Positive Integer restockedQuantity,
        @PositiveOrZero Integer damagedQuantity
) {
}
