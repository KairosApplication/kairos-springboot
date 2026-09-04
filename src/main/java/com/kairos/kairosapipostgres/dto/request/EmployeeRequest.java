package com.kairos.kairosapipostgres.dto.request;

import com.kairos.kairosapipostgres.model.enums.Position;
import jakarta.validation.constraints.NotNull;

public record EmployeeRequest(
        @NotNull(message = "O usuário é obrigatório")
        Long userId,

        @NotNull(message = "O cargo é obrigatório")
        Position position
) {
}
