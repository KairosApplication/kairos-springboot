package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SectorRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100)
        String name,
        @NotBlank(message = "O tipo é obrigatório")
        @Size(max = 255)
        String type
) {
}
