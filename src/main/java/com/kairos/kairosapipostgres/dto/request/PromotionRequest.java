package com.kairos.kairosapipostgres.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionRequest(
        @Positive Long shelfId,
        @Positive Long productId,
        @NotNull LocalDateTime promotionStartDate,
        @NotNull LocalDateTime promotionEndDate,
        String description,
        @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2)
        BigDecimal discountPercentage
) {
}
