package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SectorUpdateRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "O nome não pode estar em branco")
        @Size(max = 100)
        String name,
        @Pattern(regexp = "(?s).*\\S.*", message = "O tipo não pode estar em branco")
        @Size(max = 255)
        String type
) {
}
