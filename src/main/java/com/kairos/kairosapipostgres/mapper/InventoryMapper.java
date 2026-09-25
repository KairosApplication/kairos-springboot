package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.response.InventoryResponse;
import com.kairos.kairosapipostgres.model.Inventory;
import com.kairos.kairosapipostgres.model.Sector;

public final class InventoryMapper {
    private InventoryMapper() {
    }

    public static Inventory toEntity(Sector sector) {
        return new Inventory(null, sector);
    }

    public static InventoryResponse toResponse(Inventory inventory) {
        Sector sector = inventory.getSector();
        return new InventoryResponse(inventory.getId(),
                sector == null ? null : sector.getId(), sector == null ? null : sector.getName());
    }
}
