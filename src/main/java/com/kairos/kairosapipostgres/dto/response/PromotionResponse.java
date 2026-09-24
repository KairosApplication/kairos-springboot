package com.kairos.kairosapipostgres.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionResponse(Long id, Long shelfId, Long productId,
                                LocalDateTime promotionStartDate, LocalDateTime promotionEndDate,
                                String description, BigDecimal discountPercentage) {
}
