package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.SectorRequest;
import com.kairos.kairosapipostgres.dto.response.SectorResponse;
import com.kairos.kairosapipostgres.model.Sector;

public final class SectorMapper {

    private SectorMapper() {
        
    }

    public static Sector toEntity(SectorRequest request) {
        Sector sector = new Sector();
        sector.setName(request.name().trim());
        sector.setType(request.type().trim());
        return sector;
    }

    public static SectorResponse toResponse(Sector sector) {
        return new SectorResponse(
                sector.getId(),
                sector.getName(),
                sector.getType ()
        );
    }
    
}
