package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.AisleRequest;
import com.kairos.kairosapipostgres.dto.response.AisleResponse;
import com.kairos.kairosapipostgres.model.Aisle;
import com.kairos.kairosapipostgres.model.Sector;

public final class AisleMapper {


    private AisleMapper() {
    }

    public static Aisle toEntity(AisleRequest request, Sector sector) {
        Aisle aisle = new Aisle();
        aisle.setSector(sector);
        aisle.setPosition(request.position());
        return aisle;
    }

    public static AisleResponse toResponse(Aisle aisle) {
        return new AisleResponse(
                aisle.getId(),
                aisle.getSector().getId(),
                aisle.getSector().getName(),
                aisle.getPosition()
        );
    }

}
