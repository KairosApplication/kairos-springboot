package com.kairos.kairosapipostgres.dto.response;

public record RecommendedProductResponse(Long productId, String productName,
                                         Long categoryId, String categoryName) {
}
