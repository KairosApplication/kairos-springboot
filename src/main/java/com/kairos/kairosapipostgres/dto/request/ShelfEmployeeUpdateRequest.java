package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.Positive;

public record ShelfEmployeeUpdateRequest(
        @Positive(message = "O funcionário deve ter um ID positivo") Long employeeId,
        @Positive(message = "A prateleira deve ter um ID positivo") Long shelfId
) {
}
