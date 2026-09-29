package com.kairos.kairosapipostgres.dto.request;

import com.kairos.kairosapipostgres.model.enums.AlertStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AlertUpdateRequest(
        LocalDate stockoutDate,
        @Positive Long employeeId,
        @Positive Long shelfId,
        @Positive Long productId,
        @Pattern(regexp = "(?s).*\\S.*") @Size(max = 250) String description,
        AlertStatus status,
        LocalDateTime resolutionDate
) {
}
