package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record CustomerDamageReportRequest(
        @NotNull UUID commandId,
        @NotNull @Positive Long productId,
        @Positive Long shelfId,
        @NotBlank @Pattern(regexp = "DAMAGED|TAMPERED|EXPIRED|OTHER") String problemType,
        @NotNull @Size(max = 500) String description,
        @Size(max = 4194304) String imageBase64) {
}
