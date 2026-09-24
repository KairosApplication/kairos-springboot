package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ShelfEmployeeRequest(
        @NotNull(message = "O funcionário é obrigatório")
        @Positive(message = "O funcionário deve ter um ID positivo") Long employeeId,
        @NotNull(message = "A prateleira é obrigatória")
        @Positive(message = "A prateleira deve ter um ID positivo") Long shelfId
) {
}
