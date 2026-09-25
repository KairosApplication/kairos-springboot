package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.ShelfRequest;
import com.kairos.kairosapipostgres.dto.response.ShelfResponse;
import com.kairos.kairosapipostgres.model.Aisle;
import com.kairos.kairosapipostgres.model.Shelf;

public final class ShelfMapper {
    private ShelfMapper() {
    }

    public static Shelf toEntity(ShelfRequest request, Aisle aisle) {
        Shelf shelf = new Shelf();
        shelf.setAisle(aisle);
        shelf.setMaximumCapacity(request.maximumCapacity());
        return shelf;
    }

    public static ShelfResponse toResponse(Shelf shelf) {
        return new ShelfResponse(shelf.getId(), shelf.getAisle().getId(),
                shelf.getAisle().getPosition(), shelf.getMaximumCapacity());
    }
}
