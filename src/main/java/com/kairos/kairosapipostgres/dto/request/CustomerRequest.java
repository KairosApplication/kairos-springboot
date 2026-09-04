package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.NotNull;

public record CustomerRequest(
        @NotNull(message = "O usuário é obrigatório")
        Long userId
) {
}
