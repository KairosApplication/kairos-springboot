package com.kairos.kairosapipostgres.dto.response;

import java.time.LocalDateTime;

public record RestockingResponse(Long id, Long inventoryId, Long employeeId, Long shelfId,
                                 Long productId, LocalDateTime restockingDate,
                                 Integer restockedQuantity, Integer damagedQuantity) {
}
