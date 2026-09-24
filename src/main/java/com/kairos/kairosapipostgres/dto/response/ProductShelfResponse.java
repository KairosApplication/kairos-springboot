package com.kairos.kairosapipostgres.dto.response;

public record ProductShelfResponse(Long id, Long productId, Long shelfId, Integer productQuantity) {
}
