package com.kairos.kairosapipostgres.dto.response;

import java.util.List;

public record StoreMapResponse(List<SectorResponse> sectors, List<AisleResponse> aisles,
                               List<ShelfResponse> shelves, List<ProductResponse> products,
                               List<ProductShelfResponse> productShelves) {
    public StoreMapResponse {
        sectors = List.copyOf(sectors);
        aisles = List.copyOf(aisles);
        shelves = List.copyOf(shelves);
        products = List.copyOf(products);
        productShelves = List.copyOf(productShelves);
    }
}
