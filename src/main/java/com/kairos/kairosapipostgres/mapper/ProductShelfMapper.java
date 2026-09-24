package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.ProductShelfRequest;
import com.kairos.kairosapipostgres.dto.response.ProductShelfResponse;
import com.kairos.kairosapipostgres.model.Shelf;
import com.kairos.kairosapipostgres.model.Product;
import com.kairos.kairosapipostgres.model.ProductShelf;

public final class ProductShelfMapper {
    private ProductShelfMapper() {
    }

    public static ProductShelf toEntity(ProductShelfRequest request, Product product, Shelf shelf) {
        return new ProductShelf(null, product, shelf, request.productQuantity());
    }

    public static ProductShelfResponse toResponse(ProductShelf assignment) {
        return new ProductShelfResponse(assignment.getId(), assignment.getProduct().getId(),
                assignment.getShelf().getId(), assignment.getProductQuantity());
    }
}
