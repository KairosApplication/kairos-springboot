package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EmployeeAisleRequest(
        @NotNull(message = "O funcionário é obrigatório")
        @Positive(message = "O funcionário deve ter um ID positivo") Long employeeId,
        @NotNull(message = "O corredor é obrigatório")
        @Positive(message = "O corredor deve ter um ID positivo") Long aisleId
) {
}
