package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionUpdateRequest(
        @Positive Long shelfId,
        @Positive Long productId,
        LocalDateTime promotionStartDate,
        LocalDateTime promotionEndDate,
        String description,
        @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal discountPercentage
) {
}
