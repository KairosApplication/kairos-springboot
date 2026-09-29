package com.kairos.kairosapipostgres.dto.response;

import java.math.BigDecimal;

public record PurchaseTotalResponse(Long purchaseId, BigDecimal total) {
}
