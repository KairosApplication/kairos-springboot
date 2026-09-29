package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.RestockingRequest;
import com.kairos.kairosapipostgres.dto.response.RestockingResponse;
import com.kairos.kairosapipostgres.model.*;

import java.time.LocalDateTime;

public final class RestockingMapper {
    private RestockingMapper() {
    }

    public static Restocking toEntity(RestockingRequest request, Inventory inventory,
                                      Employee employee, Shelf shelf, Product product) {
        return new Restocking(null, inventory, employee, shelf, product, LocalDateTime.now(),
                request.restockedQuantity(), request.damagedQuantity() == null
                        ? Integer.valueOf(0) : request.damagedQuantity());
    }

    public static RestockingResponse toResponse(Restocking restocking) {
        return new RestockingResponse(restocking.getId(), restocking.getInventory().getId(),
                restocking.getEmployee().getId(), restocking.getShelf().getId(), restocking.getProduct().getId(),
                restocking.getRestockingDate(), restocking.getRestockedQuantity(), restocking.getDamagedQuantity());
    }
}
