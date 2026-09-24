package com.kairos.kairosapipostgres.dto.response;

import com.kairos.kairosapipostgres.model.enums.AlertStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AlertResponse(Long id, LocalDate stockoutDate, Long employeeId, Long shelfId,
                            Long productId, String description, AlertStatus status,
                            LocalDateTime resolutionDate) {
}
