package com.kairos.kairosapipostgres.dto.request;

import com.kairos.kairosapipostgres.model.enums.AlertStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AlertRequest(
        LocalDate stockoutDate,
        @Positive Long employeeId,
        @NotNull @Positive Long shelfId,
        @Positive Long productId,
        @NotBlank @Size(max = 250) String description,
        @NotNull AlertStatus status,
        LocalDateTime resolutionDate
) {
}
