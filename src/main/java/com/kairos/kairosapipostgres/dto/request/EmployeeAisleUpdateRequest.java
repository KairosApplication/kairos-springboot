package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.Positive;

public record EmployeeAisleUpdateRequest(
        @Positive(message = "O funcionário deve ter um ID positivo") Long employeeId,
        @Positive(message = "O corredor deve ter um ID positivo") Long aisleId
) {
}
