package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.PromotionRequest;
import com.kairos.kairosapipostgres.dto.response.PromotionResponse;
import com.kairos.kairosapipostgres.model.Product;
import com.kairos.kairosapipostgres.model.Promotion;
import com.kairos.kairosapipostgres.model.Shelf;

public final class PromotionMapper {
    private PromotionMapper() {
    }

    public static Promotion toEntity(PromotionRequest request, Shelf shelf, Product product) {
        return new Promotion(null, shelf, product, request.promotionStartDate(),
                request.promotionEndDate(), request.description(), request.discountPercentage());
    }

    public static PromotionResponse toResponse(Promotion promotion) {
        return new PromotionResponse(promotion.getId(),
                promotion.getShelf() == null ? null : promotion.getShelf().getId(),
                promotion.getProduct() == null ? null : promotion.getProduct().getId(),
                promotion.getPromotionStartDate(), promotion.getPromotionEndDate(),
                promotion.getDescription(), promotion.getDiscountPercentage());
    }
}
