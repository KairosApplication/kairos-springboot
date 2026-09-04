package com.kairos.kairosapipostgres.dto.request;

import com.kairos.kairosapipostgres.model.enums.Position;
import jakarta.validation.constraints.NotNull;

public record EmployeeUpdateRequest(
        @NotNull(message = "O cargo é obrigatório")
        Position position
) {
}
